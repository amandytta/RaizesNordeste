package br.com.raizesnordeste.raizesnordeste.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UnidadeRequestDTO(

        @NotBlank
        @Size(max = 100)
        String nome,

        @NotBlank
        @Pattern(regexp = "\\d{14}",
                message = "O CNPJ deve conter 14 dígitos")
        String cnpj,

        @Size(max = 20)
        String telefone,

        @Email
        @Size(max = 150)
        String email,

        @NotBlank
        @Pattern(regexp = "\\d{8}",
                message = "O CEP deve conter 8 dígitos")
        String cep,

        @NotBlank
        @Size(max = 150)
        String logradouro,

        @NotBlank
        @Size(max = 20)
        String numero,

        @Size(max = 100)
        String complemento,

        @NotBlank
        @Size(max = 100)
        String bairro,

        @NotBlank
        @Size(max = 100)
        String cidade,

        @NotBlank
        @Pattern(regexp = "[A-Z]{2}",
                message = "Informe a sigla do estado com duas letras maiúsculas")
        String estado

) {
}