package com.perussi.service;


import com.perussi.dto.AtualizarStatusPedidoDTO;
import com.perussi.dto.EnderecoPedidoDTO;
import com.perussi.dto.PedidoDTO;
import com.perussi.dto.PedidoItemDTO;
import com.perussi.model.CarrinhoItem;
import com.perussi.model.Pedido;
import com.perussi.model.PedidoItem;
import com.perussi.model.ProdutoVariacao;
import com.perussi.model.StatusPedido;
import com.perussi.model.Usuario;
import com.perussi.repository.CarrinhoItemRepository;
import com.perussi.repository.PedidoRepository;
import com.perussi.repository.ProdutoVariacaoRepository;
import com.perussi.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CarrinhoItemRepository carrinhoItemRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoVariacaoRepository produtoVariacaoRepository;
    private final PagamentoService pagamentoService;
    private final EntregaService entregaService;

    @org.springframework.beans.factory.annotation.Value("${pedidos.reserva.minutos:30}")
    private long reservaMinutos = 30;

    public PedidoService(
            PedidoRepository pedidoRepository,
            CarrinhoItemRepository carrinhoItemRepository,
            UsuarioRepository usuarioRepository,
            ProdutoVariacaoRepository produtoVariacaoRepository,
            PagamentoService pagamentoService, EntregaService entregaService) {
        this.pedidoRepository = pedidoRepository;
        this.carrinhoItemRepository = carrinhoItemRepository;
        this.usuarioRepository = usuarioRepository;
        this.produtoVariacaoRepository = produtoVariacaoRepository;
        this.pagamentoService = pagamentoService;
        this.entregaService = entregaService;
    }

    @Transactional
    public PedidoDTO finalizarPedido(String emailUsuario, EnderecoPedidoDTO endereco) {
        validarEndereco(endereco);
        var entrega = entregaService.consultar(endereco.cep());
        if (!"Curitiba".equalsIgnoreCase(endereco.cidade().trim()) || !"PR".equalsIgnoreCase(endereco.estado().trim()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A cidade e o estado devem corresponder ao CEP de Curitiba (PR).");

        Usuario usuario = usuarioRepository.findLockedByEmail(emailUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (!Boolean.TRUE.equals(usuario.getEmailConfirmado()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Confirme seu e-mail antes de finalizar a compra. Você pode reenviar o link pelo perfil.");

        List<CarrinhoItem> carrinho = carrinhoItemRepository.findByUsuarioId(usuario.getId());

        if (carrinho.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrinho vazio");
        }

        BigDecimal total = BigDecimal.ZERO;

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .status(StatusPedido.PENDENTE)
                .criadoEm(LocalDateTime.now())
                .reservaExpiraEm(LocalDateTime.now().plusMinutes(Math.max(1, reservaMinutos)))
                .cep(entrega.cep())
                .logradouro(endereco.logradouro())
                .numero(endereco.numero())
                .bairro(endereco.bairro())
                .cidade(entrega.cidade())
                .estado(entrega.estado())
                .complemento(endereco.complemento())
                .itens(new ArrayList<>())
                .build();

        carrinho.sort(java.util.Comparator.comparing(item -> item.getVariacao().getId()));
        for (CarrinhoItem item : carrinho) {
            ProdutoVariacao variacao = item.getVariacao();

            if (item.getQuantidade() <= 0 || produtoVariacaoRepository.reservar(variacao.getId(), item.getQuantidade()) != 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Estoque insuficiente para " + item.getProduto().getNome());
            }


            Integer desconto = item.getProduto().getDesconto() == null
                    ? 0
                    : item.getProduto().getDesconto();

            BigDecimal precoComDesconto = item.getProduto()
                    .getPreco()
                    .multiply(BigDecimal.valueOf(100 - desconto))
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

            BigDecimal subtotal = precoComDesconto
                    .multiply(BigDecimal.valueOf(item.getQuantidade()));

            total = total.add(subtotal);

            PedidoItem pedidoItem = PedidoItem.builder()
                    .pedido(pedido)
                    .produto(item.getProduto())
                    .variacao(variacao)
                    .quantidade(item.getQuantidade())
                    .precoUnitario(precoComDesconto)
                    .build();

            pedido.getItens().add(pedidoItem);
        }

        pedido.setFrete(entrega.frete());
        pedido.setTotal(total.add(entrega.frete()));

        Pedido salvo = pedidoRepository.save(pedido);

        carrinhoItemRepository.deleteAll(carrinho);

        return converterParaDTO(salvo);
    }

    public EntregaService.Entrega consultarEntrega(String cep) { return entregaService.consultar(cep); }

    public List<PedidoDTO> listarMeusPedidos(String emailUsuario) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

        return pedidoRepository.findByUsuarioId(usuario.getId())
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    public List<PedidoDTO> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public PedidoDTO atualizarStatus(UUID pedidoId, AtualizarStatusPedidoDTO dto) {
        Pedido pedido = pedidoRepository.findLockedById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido não encontrado"));

        StatusPedido novoStatus = converterStatus(dto.status());
        if (novoStatus == pedido.getStatus()) return converterParaDTO(pedido);
        if (novoStatus == StatusPedido.CANCELADO) {
            return converterParaDTO(pagamentoService.cancelar(pedidoId));
        }
        boolean permitido = (pedido.getStatus() == StatusPedido.PAGO && novoStatus == StatusPedido.ENVIADO)
                || (pedido.getStatus() == StatusPedido.ENVIADO && novoStatus == StatusPedido.ENTREGUE);
        if (!permitido) throw new ResponseStatusException(HttpStatus.CONFLICT, "Transição de pedido não permitida");
        pedido.setStatus(novoStatus);

        Pedido salvo = pedidoRepository.save(pedido);

        return converterParaDTO(salvo);
    }

    public PedidoDTO buscarMeuPedido(String emailUsuario, UUID pedidoId) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido não encontrado"));

        if (!pedido.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não possui acesso a este pedido");
        }

        return converterParaDTO(pedido);
    }

    private Usuario buscarUsuarioPorEmail(String emailUsuario) {
        return usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"));
    }

    private StatusPedido converterStatus(String status) {
        try {
            return StatusPedido.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status inválido. Use: PENDENTE, PAGO, ENVIADO, ENTREGUE ou CANCELADO");
        }
    }

    private void validarEndereco(EnderecoPedidoDTO endereco) {
        if (endereco == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Endereço é obrigatório");
        }

        if (campoVazio(endereco.cep())
                || campoVazio(endereco.logradouro())
                || campoVazio(endereco.numero())
                || campoVazio(endereco.bairro())
                || campoVazio(endereco.cidade())
                || campoVazio(endereco.estado())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CEP, logradouro, número, bairro, cidade e estado são obrigatórios");
        }
    }

    private boolean campoVazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private PedidoDTO converterParaDTO(Pedido pedido) {
        List<PedidoItemDTO> itens = pedido.getItens()
                .stream()
                .map(item -> new PedidoItemDTO(
                        item.getProduto().getId(),
                        item.getProduto().getNome(),
                        item.getVariacao().getTamanho(),
                        item.getVariacao().getCor(),
                        item.getQuantidade(),
                        item.getPrecoUnitario()))
                .toList();

        return new PedidoDTO(
                pedido.getId(),
                pedido.getTotal(),
                pedido.getTotal().subtract(pedido.getFrete() == null ? BigDecimal.ZERO : pedido.getFrete()),
                pedido.getFrete(),
                pedido.getStatus().name(),
                pedido.getMetodoPagamento(),
                pedido.getCriadoEm(),
                pedido.getCep(),
                pedido.getLogradouro(),
                pedido.getNumero(),
                pedido.getBairro(),
                pedido.getCidade(),
                pedido.getEstado(),
                pedido.getComplemento(),
                itens);
    }
}
