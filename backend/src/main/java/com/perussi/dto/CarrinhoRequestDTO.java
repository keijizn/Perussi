package com.perussi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CarrinhoRequestDTO(

        @NotNull(message = "Produto é obrigatório")
        UUID produtoId,

        @NotNull(message = "Variação é obrigatória")
        UUID variacaoId,

        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que zero")
        Integer quantidade
) {
}