package com.perussi.dto;

public record AtualizarUsuarioDTO(
        @jakarta.validation.constraints.Pattern(regexp = "(?s).*\\S.*", message = "Nome não pode ficar vazio") String nome,
        @jakarta.validation.constraints.Email(message = "Email inválido")
        @jakarta.validation.constraints.Pattern(regexp = ".*\\S.*", message = "Email não pode ficar vazio") String email,
        @jakarta.validation.constraints.Pattern(regexp = "(?s).*\\S.*", message = "A nova senha não pode conter apenas espaços")
        @jakarta.validation.constraints.Size(min = 6, max = 72, message = "A nova senha deve ter entre 6 e 72 caracteres") String senha,
        String senhaAtual
) {
}
