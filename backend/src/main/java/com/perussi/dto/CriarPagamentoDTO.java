package com.perussi.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarPagamentoDTO(
        @NotNull UUID pedidoId,
        @NotBlank String metodoPagamento
) {
}
