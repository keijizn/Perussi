package com.perussi.service;

import com.perussi.dto.CriarPagamentoDTO;
import com.perussi.model.*;
import com.perussi.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PagamentoServiceTest {
    private final PedidoRepository repository = mock(PedidoRepository.class);
    private final ProdutoVariacaoRepository variacoes = mock(ProdutoVariacaoRepository.class);
    private final PixGateway gateway = mock(PixGateway.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private PagamentoService service;
    private Pedido pedido;
    private UUID variacaoId;

    @BeforeEach
    void setup() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new PagamentoService(repository, variacoes, gateway, manager);
        variacaoId = UUID.randomUUID();
        pedido = Pedido.builder().id(UUID.randomUUID()).status(StatusPedido.PENDENTE)
                .total(new BigDecimal("10.00")).reservaExpiraEm(LocalDateTime.now().plusMinutes(30))
                .usuario(Usuario.builder().email("dono@example.com").build())
                .itens(List.of(PedidoItem.builder().quantidade(1)
                        .variacao(ProdutoVariacao.builder().id(variacaoId).build()).build())).build();
        when(repository.findLockedById(pedido.getId())).thenReturn(Optional.of(pedido));
        when(repository.findIdByPaymentId("123")).thenReturn(Optional.of(pedido.getId()));
        when(variacoes.liberar(variacaoId, 1)).thenReturn(1);
    }

    private PixGateway.Pagamento payment(String status) {
        return new PixGateway.Pagamento("123", status, pedido.getTotal(), pedido.getId().toString(), "qr", "base64");
    }

    @Test void rejectsAnotherCustomer() {
        var error = assertThrows(ResponseStatusException.class, () -> service.criarPagamento("outro@example.com",
                new CriarPagamentoDTO(pedido.getId(), "PIX")));
        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(gateway);
    }

    @Test void reconciliationApprovesOnlyAfterRemoteConfirmationAndSkipsPaidOrders() {
        pedido.setMercadoPagoPaymentId("123");
        when(gateway.consultar("123")).thenReturn(payment("pending"), payment("approved"));
        service.reconciliarPendente(pedido.getId());
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
        service.reconciliarPendente(pedido.getId());
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
        service.reconciliarPendente(pedido.getId());
        verify(gateway, times(2)).consultar("123");
        verifyNoInteractions(variacoes);
    }

    @Test void reconciliationFailureOrMismatchNeverApprovesOrReleasesStock() {
        pedido.setMercadoPagoPaymentId("123");
        when(gateway.consultar("123")).thenThrow(new IllegalStateException("offline"))
                .thenReturn(new PixGateway.Pagamento("123", "approved", BigDecimal.ONE, pedido.getId().toString(), null, null));
        assertThrows(IllegalStateException.class, () -> service.reconciliarPendente(pedido.getId()));
        assertThrows(ResponseStatusException.class, () -> service.reconciliarPendente(pedido.getId()));
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
        verifyNoInteractions(variacoes);
    }

    @Test void rejectsPaidOrder() {
        pedido.setStatus(StatusPedido.PAGO);
        assertThrows(ResponseStatusException.class, () -> service.criarPagamento("dono@example.com",
                new CriarPagamentoDTO(pedido.getId(), "PIX")));
        verifyNoInteractions(gateway);
    }

    @Test void reusesExistingPayment() {
        when(gateway.criar(pedido)).thenReturn(payment("pending"));
        when(gateway.consultar("123")).thenReturn(payment("pending"));
        var dto = new CriarPagamentoDTO(pedido.getId(), "PIX");
        assertEquals(service.criarPagamento("dono@example.com", dto), service.criarPagamento("dono@example.com", dto));
        verify(gateway, times(1)).criar(pedido);
        verify(gateway).consultar("123");
    }

    @Test void cannotResumeExpiredReservationOrCancelledOrder() {
        pedido.setMercadoPagoPaymentId("123");
        pedido.setReservaExpiraEm(LocalDateTime.now().minusSeconds(1));
        var dto = new CriarPagamentoDTO(pedido.getId(), "PIX");
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.criarPagamento("dono@example.com", dto)).getStatusCode().value());
        pedido.setStatus(StatusPedido.CANCELADO);
        assertThrows(ResponseStatusException.class, () -> service.criarPagamento("dono@example.com", dto));
        verifyNoInteractions(gateway, variacoes);
    }

    @Test void repeatedApprovalNeverRegressesDeliveredOrder() {
        pedido.setMercadoPagoPaymentId("123");
        when(gateway.consultar("123")).thenReturn(payment("approved"));
        service.sincronizar("123");
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
        pedido.setStatus(StatusPedido.ENTREGUE);
        service.sincronizar("123");
        assertEquals(StatusPedido.ENTREGUE, pedido.getStatus());
        verifyNoInteractions(variacoes);
    }

    @Test void cancellationReleasesStockOnlyOnce() {
        pedido.setMercadoPagoPaymentId("123");
        when(gateway.consultar("123")).thenReturn(payment("pending"));
        when(gateway.cancelar("123")).thenReturn(payment("cancelled"));
        service.cancelar(pedido.getId());
        service.cancelar(pedido.getId());
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        verify(variacoes, times(1)).liberar(variacaoId, 1);
    }

    @Test void approvalDuringCancellationPreservesStockAndPayment() {
        pedido.setMercadoPagoPaymentId("123");
        when(gateway.consultar("123")).thenReturn(payment("pending"));
        when(gateway.cancelar("123")).thenReturn(payment("approved"));
        service.cancelar(pedido.getId());
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
        verifyNoInteractions(variacoes);
    }

    @Test void providerFailureKeepsReservation() {
        pedido.setMercadoPagoPaymentId("123");
        when(gateway.consultar("123")).thenThrow(new IllegalStateException("offline"));
        assertThrows(IllegalStateException.class, () -> service.cancelar(pedido.getId()));
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
        verifyNoInteractions(variacoes);
    }

    @Test void uncertainCreationIsNotAssumedUnpaid() {
        pedido.setPagamentoSolicitado(true);
        when(gateway.recuperar(pedido)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.cancelar(pedido.getId()));
        verifyNoInteractions(variacoes);
    }

    @Test void expiresUnpaidOrderWithoutPix() {
        pedido.setReservaExpiraEm(LocalDateTime.now().minusMinutes(1));
        service.expirar(pedido.getId());
        service.expirar(pedido.getId());
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        verify(variacoes).liberar(variacaoId, 1);
    }

    @Test void doesNotExpirePaidOrUnexpiredOrder() {
        service.expirar(pedido.getId());
        pedido.setReservaExpiraEm(LocalDateTime.now().minusMinutes(1));
        pedido.setStatus(StatusPedido.PAGO);
        service.expirar(pedido.getId());
        verifyNoInteractions(variacoes, gateway);
    }

    @Test void recoveredWebhookLinksPaymentAfterLocalSaveFailure() {
        when(repository.findIdByPaymentId("123")).thenReturn(Optional.empty());
        when(gateway.consultar("123")).thenReturn(payment("approved"));
        pedido.setPagamentoSolicitado(true);
        service.sincronizar("123");
        assertEquals("123", pedido.getMercadoPagoPaymentId());
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
    }

    @Test void mismatchedAmountCannotApproveOrder() {
        when(gateway.consultar("123")).thenReturn(new PixGateway.Pagamento("123", "approved",
                BigDecimal.ONE, pedido.getId().toString(), null, null));
        assertThrows(ResponseStatusException.class, () -> service.sincronizar("123"));
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
    }
}
