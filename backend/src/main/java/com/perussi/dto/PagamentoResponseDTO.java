package com.perussi.dto;

public record PagamentoResponseDTO(
        String paymentId,
        String status,
        String qrCode,
        String qrCodeBase64
) {
}