package com.perussi.controller;

import com.perussi.dto.AtualizarQuantidadeCarrinhoDTO;
import com.perussi.dto.CarrinhoItemDTO;
import com.perussi.dto.CarrinhoRequestDTO;
import com.perussi.service.CarrinhoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/carrinho")
@CrossOrigin("*")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;

    public CarrinhoController(CarrinhoService carrinhoService) {
        this.carrinhoService = carrinhoService;
    }

    @PostMapping
    public CarrinhoItemDTO adicionarItem(
            Authentication authentication,
            @Valid @RequestBody CarrinhoRequestDTO dto
    ) {
        return carrinhoService.adicionarItem(authentication.getName(), dto);
    }

    @GetMapping
    public List<CarrinhoItemDTO> listarItens(Authentication authentication) {
        return carrinhoService.listarItens(authentication.getName());
    }

    @PutMapping("/{itemId}")
    public CarrinhoItemDTO atualizarQuantidade(
            Authentication authentication,
            @PathVariable UUID itemId,
            @Valid @RequestBody AtualizarQuantidadeCarrinhoDTO dto
    ) {
        return carrinhoService.atualizarQuantidade(
                authentication.getName(),
                itemId,
                dto.quantidade()
        );
    }

    @DeleteMapping("/{itemId}")
    public void removerItem(
            Authentication authentication,
            @PathVariable UUID itemId
    ) {
        carrinhoService.removerItem(authentication.getName(), itemId);
    }
}