package br.com.raizesnordeste.raizesnordeste.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// representa os dados necessários para o cadastro administrativo de funcionários
public record FuncionarioCadastroRequestDTO(

        // valida o nome do funcionário
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres.")
        String nome,

        // valida o endereço de e-mail
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        @Size(max = 150, message = "O e-mail deve possuir no máximo 150 caracteres.")
        String email,

        // valida o tamanho da senha
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72,
                message = "A senha deve possuir entre 8 e 72 caracteres.")
        String senha,

        // permite exclusivamente os perfis de funcionários
        @NotBlank(message = "O perfil é obrigatório.")
        @Pattern(
                regexp = "ATENDENTE|GERENTE",
                message = "O perfil deve ser ATENDENTE ou GERENTE."
        )
        String perfil

) {

    // evita a exposição de dados sensíveis em logs
    @Override
    public String toString() {
        return "FuncionarioCadastroRequestDTO[dados protegidos]";
    }
}