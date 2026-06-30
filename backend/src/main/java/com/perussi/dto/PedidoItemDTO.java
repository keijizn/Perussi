package com.perussi.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PedidoItemDTO(
        UUID produtoId,
        String nomeProduto,
        String tamanho,
        String cor,
        Integer quantidade,
        BigDecimal precoUnitario
) {}