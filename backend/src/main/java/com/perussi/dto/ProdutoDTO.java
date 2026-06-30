package com.perussi.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProdutoDTO(
        UUID id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer desconto,
        String categoria,
        String imagemUrl,
        Boolean destaque,
        List<ProdutoVariacaoDTO> variacoes
) {}