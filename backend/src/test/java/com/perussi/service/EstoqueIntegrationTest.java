package com.perussi.service;

import com.perussi.dto.EnderecoPedidoDTO;
import com.perussi.model.*;
import com.perussi.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({PedidoService.class, PagamentoService.class, CarrinhoService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EstoqueIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired PedidoService checkout;
    @Autowired CarrinhoService carrinho;
    @Autowired PagamentoService pagamentos;
    @Autowired ProdutoVariacaoRepository variacoes;
    @Autowired PedidoRepository pedidos;
    @MockBean PixGateway gateway;
    @MockBean EntregaService entregas;
    private final EnderecoPedidoDTO endereco = new EnderecoPedidoDTO("80020000", "Rua", "1", "Centro", "Curitiba", "PR", "");
    private TransactionTemplate tx;

    @BeforeEach void setup() {
        tx = new TransactionTemplate(manager);
        org.mockito.Mockito.when(entregas.consultar("80020000")).thenReturn(new EntregaService.Entrega("80020000", "Rua", "Centro", "Curitiba", "PR", EntregaService.FRETE));
    }

    private record Fixture(UUID variacao, String email1, String email2) {}

    private Fixture fixture(int estoque) {
        return tx.execute(ignored -> {
            Produto produto = Produto.builder().nome("Camiseta").preco(new BigDecimal("10.00"))
                    .categoria("Roupas").desconto(0).build();
            em.persist(produto);
            ProdutoVariacao variacao = ProdutoVariacao.builder().produto(produto).tamanho("M").cor("Preto")
                    .estoque(estoque).build();
            em.persist(variacao);
            String[] emails = {UUID.randomUUID() + "@example.com", UUID.randomUUID() + "@example.com"};
            for (String email : emails) {
                Usuario usuario = Usuario.builder().email(email).emailConfirmado(true).nome("Cliente").role("CUSTOMER").senha("hash").build();
                em.persist(usuario);
                em.persist(CarrinhoItem.builder().usuario(usuario).produto(produto).variacao(variacao).quantidade(1).build());
            }
            return new Fixture(variacao.getId(), emails[0], emails[1]);
        });
    }

    private boolean comprar(String email) {
        try { checkout.finalizarPedido(email, endereco); return true; }
        catch (org.springframework.web.server.ResponseStatusException expected) { return false; }
    }

    private int comprasConcorrentes(String email1, String email2) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> a = pool.submit(() -> { start.await(); return comprar(email1); });
            Future<Boolean> b = pool.submit(() -> { start.await(); return comprar(email2); });
            start.countDown();
            return (a.get(15, TimeUnit.SECONDS) ? 1 : 0) + (b.get(15, TimeUnit.SECONDS) ? 1 : 0);
        } finally { pool.shutdownNow(); }
    }

    @Test void unconfirmedAccountCannotCheckoutAndKeepsCartAndStock() {
        Fixture f = fixture(2);
        tx.executeWithoutResult(ignored -> {
            var u = em.createQuery("select u from Usuario u where u.email = :email", Usuario.class).setParameter("email", f.email1()).getSingleResult();
            u.setEmailConfirmado(null);
        });
        var erro = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> checkout.finalizarPedido(f.email1(), endereco));
        assertEquals(403, erro.getStatusCode().value());
        assertEquals(2, variacoes.findById(f.variacao()).orElseThrow().getEstoque());
        tx.executeWithoutResult(ignored -> {
            var u = em.createQuery("select u from Usuario u where u.email = :email", Usuario.class).setParameter("email", f.email1()).getSingleResult();
            assertEquals(1L, em.createQuery("select count(c) from CarrinhoItem c where c.usuario.id = :id", Long.class).setParameter("id", u.getId()).getSingleResult());
            assertEquals(0L, em.createQuery("select count(p) from Pedido p where p.usuario.id = :id", Long.class).setParameter("id", u.getId()).getSingleResult());
        });
    }

    @Test void onlyOneCustomerCanBuyLastUnit() throws Exception {
        Fixture f = fixture(1);
        assertEquals(1, comprasConcorrentes(f.email1(), f.email2()));
        assertEquals(0, variacoes.findById(f.variacao()).orElseThrow().getEstoque());
    }

    @Test void doubleCheckoutDoesNotCreateTwoOrdersFromSameCart() throws Exception {
        Fixture f = fixture(2);
        assertEquals(1, comprasConcorrentes(f.email1(), f.email1()));
        assertEquals(1, variacoes.findById(f.variacao()).orElseThrow().getEstoque());
    }

    @Test void repeatedCancellationRestoresStockOnce() {
        Fixture f = fixture(1);
        UUID id = checkout.finalizarPedido(f.email1(), endereco).id();
        pagamentos.cancelar(id);
        pagamentos.cancelar(id);
        assertEquals(1, variacoes.findById(f.variacao()).orElseThrow().getEstoque());
        assertEquals(StatusPedido.CANCELADO, pedidos.findById(id).orElseThrow().getStatus());
    }

    @Test void pendingOrderCannotBeMarkedPaidByStatusUpdate() {
        Fixture f = fixture(1);
        UUID id = checkout.finalizarPedido(f.email1(), endereco).id();
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> checkout.atualizarStatus(id, new com.perussi.dto.AtualizarStatusPedidoDTO("PAGO")));
        assertEquals(StatusPedido.PENDENTE, pedidos.findById(id).orElseThrow().getStatus());
    }

    @Test void failedTransactionRollsBackAtomicStockUpdate() {
        Fixture f = fixture(1);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(ignored -> {
            assertEquals(1, variacoes.reservar(f.variacao(), 1));
            throw new IllegalStateException("falha após reservar");
        }));
        assertEquals(1, variacoes.findById(f.variacao()).orElseThrow().getEstoque());
    }

    @Test void rejectedDeliveryDoesNotReserveStockOrEmptyCart() {
        Fixture f = fixture(2);
        org.mockito.Mockito.when(entregas.consultar("01001000")).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Fora de Curitiba"));
        var forged = new EnderecoPedidoDTO("01001000", "Rua", "1", "Centro", "Curitiba", "PR", "");
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> checkout.finalizarPedido(f.email1(), forged));
        assertEquals(2, variacoes.findById(f.variacao()).orElseThrow().getEstoque());
        assertEquals(1, tx.execute(ignored -> carrinho.listarItens(f.email1())).size());
    }

    @Test void paymentReceivesTotalIncludingFreight() {
        Fixture f = fixture(2);
        var pedido = checkout.finalizarPedido(f.email1(), endereco);
        org.mockito.Mockito.when(gateway.criar(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> {
            Pedido persisted = call.getArgument(0);
            assertEquals(new BigDecimal("25.00"), persisted.getTotal());
            return new PixGateway.Pagamento("123", "pending", persisted.getTotal(), persisted.getId().toString(), "qr", "");
        });
        pagamentos.criarPagamento(f.email1(), new com.perussi.dto.CriarPagamentoDTO(pedido.id(), "PIX"));
        org.mockito.Mockito.verify(gateway).criar(org.mockito.ArgumentMatchers.any());
    }

    @Test void historicalOrderKeepsOriginalTotal() {
        Fixture f = fixture(2);
        var novo = checkout.finalizarPedido(f.email1(), endereco);
        tx.executeWithoutResult(ignored -> {
            Pedido pedido = em.find(Pedido.class, novo.id());
            pedido.setFrete(null);
            pedido.setTotal(new BigDecimal("10.00"));
        });
        var antigo = tx.execute(ignored -> checkout.buscarMeuPedido(f.email1(), novo.id()));
        assertNull(antigo.frete());
        assertEquals(new BigDecimal("10.00"), antigo.total());
        assertEquals(antigo.total(), antigo.subtotal());
    }

    @Test void cartAndOrderUseSameCentRoundingForDiscounts() {
        Fixture f = fixture(2);
        tx.executeWithoutResult(ignored -> {
            Produto produto = em.find(ProdutoVariacao.class, f.variacao()).getProduto();
            produto.setPreco(new BigDecimal("9.99"));
            produto.setDesconto(15);
            CarrinhoItem item = em.createQuery("select c from CarrinhoItem c where c.usuario.email = :email", CarrinhoItem.class)
                    .setParameter("email", f.email1()).getSingleResult();
            item.setQuantidade(2);
        });
        var itens = tx.execute(ignored -> carrinho.listarItens(f.email1()));
        BigDecimal esperado = itens.get(0).precoComDesconto().multiply(BigDecimal.valueOf(2));
        assertEquals(new BigDecimal("16.98"), esperado);
        var pedido = checkout.finalizarPedido(f.email1(), endereco);
        assertEquals(esperado, pedido.subtotal());
        assertEquals(new BigDecimal("15.00"), pedido.frete());
        assertEquals(new BigDecimal("31.98"), pedido.total());
        assertEquals(pedido.total(), pedidos.findById(pedido.id()).orElseThrow().getTotal());
    }

    @Test void rollbackRestoresFirstReservationWhenAnotherItemIsUnavailable() {
        Fixture f = fixture(1);
        tx.executeWithoutResult(ignored -> {
            Usuario user = em.createQuery("select u from Usuario u where u.email = :email", Usuario.class)
                    .setParameter("email", f.email1()).getSingleResult();
            ProdutoVariacao first = em.find(ProdutoVariacao.class, f.variacao());
            ProdutoVariacao empty = ProdutoVariacao.builder().produto(first.getProduto()).tamanho("G").cor("Azul").estoque(0).build();
            em.persist(empty);
            em.persist(CarrinhoItem.builder().usuario(user).produto(first.getProduto()).variacao(empty).quantidade(1).build());
        });
        assertFalse(comprar(f.email1()));
        assertEquals(1, variacoes.findById(f.variacao()).orElseThrow().getEstoque());
    }
}
