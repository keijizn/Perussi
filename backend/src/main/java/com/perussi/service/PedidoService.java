package com.perussi.service;

import com.perussi.dto.ConfirmarPagamentoDTO;
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

    public PedidoService(
            PedidoRepository pedidoRepository,
            CarrinhoItemRepository carrinhoItemRepository,
            UsuarioRepository usuarioRepository,
            ProdutoVariacaoRepository produtoVariacaoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.carrinhoItemRepository = carrinhoItemRepository;
        this.usuarioRepository = usuarioRepository;
        this.produtoVariacaoRepository = produtoVariacaoRepository;
    }

    @Transactional
    public PedidoDTO finalizarPedido(String emailUsuario, EnderecoPedidoDTO endereco) {
        validarEndereco(endereco);

        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

        List<CarrinhoItem> carrinho = carrinhoItemRepository.findByUsuarioId(usuario.getId());

        if (carrinho.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrinho vazio");
        }

        BigDecimal total = BigDecimal.ZERO;

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .status(StatusPedido.PENDENTE)
                .criadoEm(LocalDateTime.now())
                .cep(endereco.cep())
                .logradouro(endereco.logradouro())
                .numero(endereco.numero())
                .bairro(endereco.bairro())
                .cidade(endereco.cidade())
                .estado(endereco.estado())
                .complemento(endereco.complemento())
                .itens(new ArrayList<>())
                .build();

        for (CarrinhoItem item : carrinho) {
            ProdutoVariacao variacao = item.getVariacao();

            if (item.getQuantidade() > variacao.getEstoque()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Estoque insuficiente para " + item.getProduto().getNome());
            }

            variacao.setEstoque(variacao.getEstoque() - item.getQuantidade());
            produtoVariacaoRepository.save(variacao);

            Integer desconto = item.getProduto().getDesconto() == null
                    ? 0
                    : item.getProduto().getDesconto();

            BigDecimal precoComDesconto = item.getProduto()
                    .getPreco()
                    .multiply(BigDecimal.valueOf(100 - desconto))
                    .divide(BigDecimal.valueOf(100));

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

        pedido.setTotal(total);

        Pedido salvo = pedidoRepository.save(pedido);

        carrinhoItemRepository.deleteAll(carrinho);

        return converterParaDTO(salvo);
    }

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

    public PedidoDTO atualizarStatus(UUID pedidoId, AtualizarStatusPedidoDTO dto) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido não encontrado"));

        StatusPedido novoStatus = converterStatus(dto.status());
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

    public PedidoDTO confirmarPagamento(
            String emailUsuario,
            UUID pedidoId,
            ConfirmarPagamentoDTO dto) {
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

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Apenas pedidos pendentes podem ser pagos");
        }

        String metodo = dto.metodoPagamento().toUpperCase();

        if (!metodo.equals("PIX") && !metodo.equals("CARTAO")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Método de pagamento inválido");
        }

        pedido.setMetodoPagamento(metodo);

        pedido.setStatus(StatusPedido.PAGO);

        Pedido salvo = pedidoRepository.save(pedido);

        return converterParaDTO(salvo);
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