package com.perussi.service;

import com.perussi.dto.*;
import com.perussi.model.*;
import com.perussi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PagamentoService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PagamentoService.class);
    private final PedidoRepository pedidos;
    private final ProdutoVariacaoRepository variacoes;
    private final PixGateway gateway;
    private final TransactionTemplate tx;

    public PagamentoService(PedidoRepository pedidos, ProdutoVariacaoRepository variacoes,
                            PixGateway gateway, PlatformTransactionManager manager) {
        this.pedidos = pedidos;
        this.variacoes = variacoes;
        this.gateway = gateway;
        this.tx = new TransactionTemplate(manager);
    }

    public PagamentoResponseDTO criarPagamento(String email, CriarPagamentoDTO dto) {
        // Commit intent BEFORE talking to the provider, so a timeout cannot release stock blindly.
        tx.executeWithoutResult(ignored -> {
            Pedido pedido = bloquear(dto.pedidoId());
            validarCriacao(pedido, email, dto.metodoPagamento());
            gateway.preparar(pedido);
            pedido.setPagamentoSolicitado(true);
        });
        return tx.execute(ignored -> {
            Pedido pedido = bloquear(dto.pedidoId());
            validarCriacao(pedido, email, dto.metodoPagamento());
            PixGateway.Pagamento pagamento = pedido.getMercadoPagoPaymentId() == null
                    ? gateway.criar(pedido) : gateway.consultar(pedido.getMercadoPagoPaymentId());
            aplicar(pedido, pagamento);
            return new PagamentoResponseDTO(pagamento.id(), pagamento.status(),
                    pagamento.qrCode(), pagamento.qrCodeBase64());
        });
    }

    private void validarCriacao(Pedido pedido, String email, String metodo) {
        if (!pedido.getUsuario().getEmail().equals(email)) {
            throw erro(HttpStatus.FORBIDDEN, "Você não possui acesso a este pedido");
        }
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw erro(HttpStatus.BAD_REQUEST, "Apenas pedidos pendentes podem gerar pagamento");
        }
        if (pedido.getReservaExpiraEm() != null && !pedido.getReservaExpiraEm().isAfter(LocalDateTime.now())) {
            throw erro(HttpStatus.CONFLICT, "O prazo da reserva terminou. Acompanhe o status do pedido.");
        }
        if (!"PIX".equalsIgnoreCase(metodo)) {
            throw erro(HttpStatus.BAD_REQUEST, "Apenas PIX está habilitado");
        }
    }

    public Pedido cancelar(UUID id) {
        return tx.execute(ignored -> cancelarBloqueado(bloquear(id)));
    }

    public void expirar(UUID id) {
        tx.executeWithoutResult(ignored -> {
            Pedido pedido = bloquear(id);
            if (pedido.getStatus() == StatusPedido.PENDENTE && pedido.getReservaExpiraEm() != null
                    && !pedido.getReservaExpiraEm().isAfter(LocalDateTime.now())) {
                cancelarBloqueado(pedido);
            }
        });
    }

    private Pedido cancelarBloqueado(Pedido pedido) {
        if (pedido.getStatus() == StatusPedido.CANCELADO) return pedido;
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw erro(HttpStatus.CONFLICT, "Pedido pago exige tratamento de estorno; cancelamento automático não permitido");
        }
        PixGateway.Pagamento pagamento = null;
        if (pedido.getMercadoPagoPaymentId() != null) {
            pagamento = gateway.consultar(pedido.getMercadoPagoPaymentId());
        } else if (Boolean.TRUE.equals(pedido.getPagamentoSolicitado())) {
            pagamento = gateway.recuperar(pedido).orElseThrow(() ->
                    erro(HttpStatus.SERVICE_UNAVAILABLE,
                            "Pagamento com resultado incerto. Reserva mantida até conciliação."));
        }
        if (pagamento != null) {
            validarVinculo(pedido, pagamento);
            if ("approved".equals(pagamento.status())) {
                aplicar(pedido, pagamento);
                return pedido;
            }
            if (!terminalSemCobranca(pagamento.status())) pagamento = gateway.cancelar(pagamento.id());
            aplicar(pedido, pagamento);
            if (pedido.getStatus() == StatusPedido.PAGO || pedido.getStatus() == StatusPedido.CANCELADO) return pedido;
            throw erro(HttpStatus.SERVICE_UNAVAILABLE, "PIX ainda ativo. Estoque permanece reservado.");
        }
        liberar(pedido);
        return pedido;
    }

    public void sincronizar(String paymentId) {
        tx.executeWithoutResult(ignored -> {
            // Query the provider while holding the order lock to avoid applying stale status.
            UUID pedidoId = pedidos.findIdByPaymentId(paymentId).orElse(null);
            boolean recuperarVinculo = pedidoId == null;
            if (recuperarVinculo) {
                PixGateway.Pagamento pagamento = gateway.consultar(paymentId);
                try {
                    pedidoId = UUID.fromString(pagamento.referencia());
                } catch (IllegalArgumentException | NullPointerException ignoredReference) {
                    return; // unrelated payment in this merchant account
                }
            }
            Pedido pedido = pedidos.findLockedById(pedidoId).orElse(null);
            if (pedido == null) return;
            if (recuperarVinculo && !Boolean.TRUE.equals(pedido.getPagamentoSolicitado())) {
                throw erro(HttpStatus.CONFLICT, "Pagamento sem solicitação registrada; conciliação manual necessária");
            }
            PixGateway.Pagamento pagamento = gateway.consultar(paymentId);
            aplicar(pedido, pagamento);
        });
    }

    public void reconciliarPendente(UUID id) {
        tx.executeWithoutResult(ignored -> {
            Pedido pedido = pedidos.findLockedById(id).orElse(null);
            if (pedido == null || pedido.getStatus() != StatusPedido.PENDENTE
                    || pedido.getMercadoPagoPaymentId() == null) return;
            aplicar(pedido, gateway.consultar(pedido.getMercadoPagoPaymentId()));
        });
    }

    private void validarVinculo(Pedido pedido, PixGateway.Pagamento pagamento) {
        boolean idConhecido = pagamento.id().equals(pedido.getMercadoPagoPaymentId());
        boolean referenciaCorreta = pedido.getId().toString().equals(pagamento.referencia());
        if ((!idConhecido && !referenciaCorreta)
                || (pedido.getMercadoPagoPaymentId() != null && !idConhecido)
                || pagamento.total() == null || pedido.getTotal().compareTo(pagamento.total()) != 0) {
            throw erro(HttpStatus.CONFLICT, "Pagamento divergente; conciliação manual necessária");
        }
    }

    private void aplicar(Pedido pedido, PixGateway.Pagamento pagamento) {
        validarVinculo(pedido, pagamento);
        pedido.setMercadoPagoPaymentId(pagamento.id());
        pedido.setMercadoPagoStatus(pagamento.status());
        pedido.setMetodoPagamento("PIX");
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            if (pedido.getStatus() == StatusPedido.CANCELADO && "approved".equals(pagamento.status())) {
                log.error("Pagamento aprovado após cancelamento do pedido {}: revisar e estornar manualmente", pedido.getId());
            }
            return;
        }
        if ("approved".equals(pagamento.status())) {
            pedido.setStatus(StatusPedido.PAGO);
        } else if (terminalSemCobranca(pagamento.status())) {
            liberar(pedido);
        }
    }

    private boolean terminalSemCobranca(String status) {
        return Set.of("cancelled", "rejected").contains(status == null ? "" : status);
    }

    private void liberar(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.PENDENTE) return;
        pedido.getItens().stream().sorted(Comparator.comparing(item -> item.getVariacao().getId()))
                .forEach(item -> {
                    if (variacoes.liberar(item.getVariacao().getId(), item.getQuantidade()) != 1)
                        throw erro(HttpStatus.CONFLICT, "Variação não encontrada para devolver estoque");
                });
        pedido.setStatus(StatusPedido.CANCELADO);
    }

    private Pedido bloquear(UUID id) {
        return pedidos.findLockedById(id).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
