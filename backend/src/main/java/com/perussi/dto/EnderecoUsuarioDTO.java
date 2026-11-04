package com.perussi.dto;

public record EnderecoUsuarioDTO(

        @jakarta.validation.constraints.NotBlank(message = "CEP é obrigatório")
        @jakarta.validation.constraints.Pattern(regexp = "[0-9]{5}-?[0-9]{3}", message = "CEP deve conter 8 dígitos") String cep,
        @jakarta.validation.constraints.NotBlank(message = "Logradouro é obrigatório") String logradouro,
        @jakarta.validation.constraints.NotBlank(message = "Número é obrigatório") String numero,
        @jakarta.validation.constraints.NotBlank(message = "Bairro é obrigatório") String bairro,
        @jakarta.validation.constraints.NotBlank(message = "Cidade é obrigatória") String cidade,
        @jakarta.validation.constraints.NotBlank(message = "UF é obrigatória")
        @jakarta.validation.constraints.Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO", message = "Informe uma UF válida em maiúsculas") String estado,
        String complemento

) {
}
