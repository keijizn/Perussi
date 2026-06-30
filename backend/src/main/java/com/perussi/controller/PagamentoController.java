package com.perussi.controller;

import com.perussi.dto.CriarPagamentoDTO;
import com.perussi.dto.PagamentoResponseDTO;
import com.perussi.service.PagamentoService;
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
            @RequestBody CriarPagamentoDTO dto
    ) {
        return pagamentoService.criarPagamento(dto);
    }
}