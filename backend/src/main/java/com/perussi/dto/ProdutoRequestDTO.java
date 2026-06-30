package com.perussi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record ProdutoRequestDTO(

        @NotBlank(message = "Nome do produto é obrigatório")
        String nome,

        String descricao,

        @NotNull(message = "Preço é obrigatório")
        @Positive(message = "Preço deve ser maior que zero")
        BigDecimal preco,
        Integer desconto,

        @NotBlank(message = "Categoria é obrigatória")
        String categoria,

        String imagemUrl,

        Boolean destaque,

        @Valid
        List<ProdutoVariacaoRequestDTO> variacoes
) {
}