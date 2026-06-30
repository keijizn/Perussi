package com.perussi.controller;

import com.perussi.dto.ProdutoVariacaoDTO;
import com.perussi.dto.ProdutoVariacaoRequestDTO;
import com.perussi.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/variacoes")
@CrossOrigin("*")
public class ProdutoVariacaoController {

    private final ProdutoService produtoService;

    public ProdutoVariacaoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PutMapping("/{id}")
    public ProdutoVariacaoDTO atualizarVariacao(
            @PathVariable UUID id,
            @Valid @RequestBody ProdutoVariacaoRequestDTO dto) {
        return produtoService.atualizarVariacao(id, dto);
    }

    @DeleteMapping("/{id}")
    public void excluirVariacao(@PathVariable UUID id) {
        produtoService.excluirVariacao(id);
    }

    @PostMapping
    public ProdutoVariacaoDTO criarVariacao(
            @Valid @RequestBody ProdutoVariacaoRequestDTO dto) {
        return produtoService.criarVariacao(dto);
    }
}