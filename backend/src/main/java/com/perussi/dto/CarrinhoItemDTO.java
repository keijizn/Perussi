package com.perussi.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CarrinhoItemDTO(
                UUID id,
                UUID produtoId,
                String nomeProduto,
                String imagemUrl,
                BigDecimal preco,
                Integer desconto,
                BigDecimal precoComDesconto,
                UUID variacaoId,
                String tamanho,
                String cor,
                Integer quantidade,
                Integer estoqueDisponivel) {
}