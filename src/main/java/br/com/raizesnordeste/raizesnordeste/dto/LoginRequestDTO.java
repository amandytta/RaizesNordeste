package br.com.raizesnordeste.raizesnordeste.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// representa as credenciais enviadas pelo usuario durante o login
public record LoginRequestDTO(

        // garante que o e-mail seja informado e tenha formato valido
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        // garante que a senha seja informada
        @NotBlank(message = "A senha é obrigatória.")
        @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
        String senha

) {

    // sobrescreve o toString para evitar a exposicao acidental da senha em mensagens de log.
    @Override
    public String toString() {
        return "LoginRequestDTO[credenciais protegidas]";
    }
}