package com.perussi.dto;

public record AtualizarUsuarioDTO(
        String nome,
        String email,
        String senha
) {
}