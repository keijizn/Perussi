package com.perussi.controller;

import com.perussi.dto.CriarPagamentoDTO;
import com.perussi.dto.PagamentoResponseDTO;
import com.perussi.service.PagamentoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/criar")
    public PagamentoResponseDTO criarPagamento(
            Authentication authentication,
            @Valid @RequestBody CriarPagamentoDTO dto
    ) {
        return pagamentoService.criarPagamento(authentication.getName(), dto);
    }
}
