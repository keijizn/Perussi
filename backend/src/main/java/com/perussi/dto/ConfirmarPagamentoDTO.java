package com.perussi.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmarPagamentoDTO(

        @NotBlank(message = "Método de pagamento é obrigatório")
        String metodoPagamento

) {
}