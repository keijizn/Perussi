package com.perussi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FavoritoDTO(
        UUID id,
        UUID produtoId,
        String nomeProduto,
        String descricao,
        BigDecimal preco,
        Integer desconto,
        String categoria,
        String imagemUrl,
        LocalDateTime criadoEm
) {
}