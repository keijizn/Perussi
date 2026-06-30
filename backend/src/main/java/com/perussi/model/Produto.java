package com.perussi.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import java.util.List;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private BigDecimal preco;
    private Integer desconto;

    @Column(nullable = false)
    private String categoria;

    private String imagemUrl;

    private Boolean destaque;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL)
    private List<ProdutoVariacao> variacoes;
}