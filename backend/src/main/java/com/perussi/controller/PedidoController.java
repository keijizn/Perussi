package com.perussi.controller;

import com.perussi.dto.AtualizarStatusPedidoDTO;
import com.perussi.dto.EnderecoPedidoDTO;
import com.perussi.dto.PedidoDTO;
import com.perussi.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.perussi.dto.ConfirmarPagamentoDTO;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
@CrossOrigin("*")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/finalizar")
    public PedidoDTO finalizarPedido(
            Authentication authentication,
            @Valid @RequestBody EnderecoPedidoDTO endereco) {
        return pedidoService.finalizarPedido(authentication.getName(), endereco);
    }

    @GetMapping("/meus")
    public List<PedidoDTO> listarMeusPedidos(Authentication authentication) {
        return pedidoService.listarMeusPedidos(authentication.getName());
    }

    @GetMapping
    public List<PedidoDTO> listarTodos() {
        return pedidoService.listarTodos();
    }

    @PutMapping("/{pedidoId}/status")
    public PedidoDTO atualizarStatus(
            @PathVariable UUID pedidoId,
            @Valid @RequestBody AtualizarStatusPedidoDTO dto) {
        return pedidoService.atualizarStatus(pedidoId, dto);
    }

    @GetMapping("/{pedidoId}/meu")
    public PedidoDTO buscarMeuPedido(
            Authentication authentication,
            @PathVariable UUID pedidoId) {
        return pedidoService.buscarMeuPedido(
                authentication.getName(),
                pedidoId);
    }

    @PutMapping("/{pedidoId}/pagamento")
    public PedidoDTO confirmarPagamento(
            Authentication authentication,
            @PathVariable UUID pedidoId,
            @RequestBody ConfirmarPagamentoDTO dto) {
        return pedidoService.confirmarPagamento(
                authentication.getName(),
                pedidoId,
                dto);
    }
}