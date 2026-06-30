package com.perussi.dto;

public record EnderecoUsuarioDTO(

        String cep,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String complemento

) {
}