package com.perussi.dto;

import java.util.UUID;

public record ProdutoVariacaoDTO(
        UUID id,
        String tamanho,
        String cor,
        Integer estoque
) {}