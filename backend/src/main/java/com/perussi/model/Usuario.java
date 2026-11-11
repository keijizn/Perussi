package com.perussi.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;

    @Column(unique = true)
    private String email;

    private String senha;

    private String versaoSessao;

    @Column(unique = true, length = 64)
    private String recuperacaoHash;
    private java.time.Instant recuperacaoExpiraEm;
    private java.time.Instant recuperacaoSolicitadaEm;
    private String recuperacaoVersaoSessao;

    private Boolean emailConfirmado;
    @Column(unique = true, length = 64)
    private String confirmacaoHash;
    private String confirmacaoDestino;
    private java.time.Instant confirmacaoExpiraEm;
    private java.time.Instant confirmacaoSolicitadaEm;
    private java.time.Instant confirmacaoJanelaEm;
    private Integer confirmacaoEnvios;

    private String role;
}
