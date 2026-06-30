package com.perussi.dto;

import java.util.UUID;

public record CriarPagamentoDTO(
        UUID pedidoId,
        String metodoPagamento
) {
}