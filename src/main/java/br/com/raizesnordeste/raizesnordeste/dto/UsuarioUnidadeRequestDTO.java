package br.com.raizesnordeste.raizesnordeste.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// representa os dados necessários para vincular um usuário a uma unidade
public record UsuarioUnidadeRequestDTO(

        // identifica a unidade que será vinculada ao usuário
        @NotNull(message = "O identificador da unidade é obrigatório.")
        @Positive(message = "O identificador da unidade deve ser positivo.")
        Long unidadeId

) {
}