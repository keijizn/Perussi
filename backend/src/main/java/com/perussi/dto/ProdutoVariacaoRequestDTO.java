package com.perussi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record ProdutoVariacaoRequestDTO(

        UUID produtoId,

        @NotBlank(message = "Tamanho é obrigatório")
        String tamanho,

        @NotBlank(message = "Cor é obrigatória")
        String cor,

        @NotNull(message = "Estoque é obrigatório")
        @PositiveOrZero(message = "Estoque não pode ser negativo")
        Integer estoque
) {
}