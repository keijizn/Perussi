package com.perussi.controller;

import com.perussi.dto.FavoritoDTO;
import com.perussi.service.FavoritoService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/favoritos")
@CrossOrigin("*")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @PostMapping("/{produtoId}")
    public FavoritoDTO adicionarFavorito(
            Authentication authentication,
            @PathVariable UUID produtoId
    ) {
        return favoritoService.adicionarFavorito(authentication.getName(), produtoId);
    }

    @GetMapping
    public List<FavoritoDTO> listarFavoritos(Authentication authentication) {
        return favoritoService.listarFavoritos(authentication.getName());
    }

    @DeleteMapping("/{produtoId}")
    public void removerFavorito(
            Authentication authentication,
            @PathVariable UUID produtoId
    ) {
        favoritoService.removerFavorito(authentication.getName(), produtoId);
    }
}