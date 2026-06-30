package com.perussi.controller;

import com.perussi.dto.ProdutoDTO;
import com.perussi.dto.ProdutoRequestDTO;
import com.perussi.model.ProdutoVariacao;
import com.perussi.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/produtos")
@CrossOrigin("*")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    public ProdutoDTO criar(@Valid @RequestBody ProdutoRequestDTO dto) {
        return produtoService.criar(dto);
    }

    @GetMapping
    public List<ProdutoDTO> listarTodos() {
        return produtoService.listarTodos();
    }

    @GetMapping("/{id}")
    public ProdutoDTO buscarPorId(@PathVariable UUID id) {
        return produtoService.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable UUID id) {
    produtoService.excluir(id);
    }

    @PutMapping("/{id}")
    public ProdutoDTO atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody ProdutoRequestDTO dto
    ){
    return produtoService.atualizar(id, dto);
    }

    @GetMapping("/{produtoId}/variacoes/{variacaoId}")
    public ProdutoVariacao buscarVariacao(
            @PathVariable UUID produtoId,
            @PathVariable UUID variacaoId
    ) {
        return produtoService.buscarVariacao(produtoId, variacaoId);
    }

    
}