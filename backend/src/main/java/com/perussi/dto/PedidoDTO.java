package com.perussi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PedidoDTO(

        UUID id,

        BigDecimal total,

        BigDecimal subtotal,

        BigDecimal frete,

        String status,

        String metodoPagamento,

        LocalDateTime criadoEm,

        String cep,

        String logradouro,

        String numero,

        String bairro,

        String cidade,

        String estado,

        String complemento,

        List<PedidoItemDTO> itens
) {
}