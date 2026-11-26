package br.com.raizesnordeste.raizesnordeste.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// representa os dados recebidos no cadastro publico de usuarios
public record UsuarioCadastroRequestDTO(

        // valida o nome de acordo com os limites do banco de dados
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres.")
        String nome,

        // valida a obrigatoriedade e o formato do e-mail
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        @Size(max = 150, message = "O e-mail deve possuir no máximo 150 caracteres.")
        String email,

        // define os limites de tamanho da senha
        @NotBlank(message = "A senha é obrigatória.")
        @Size(
                min = 8,
                max = 72,
                message = "A senha deve possuir entre 8 e 72 caracteres."
        )
        String senha

) {

    // evita a exposição de dados pessoais e credenciais caso o DTO seja convertido em texto
    @Override
    public String toString() {
        return "UsuarioCadastroRequestDTO[dados protegidos]";
    }
}