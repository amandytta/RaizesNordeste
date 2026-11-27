package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.enums.CanalPedido;
import br.com.raizesnordeste.raizesnordeste.enums.FinalidadeConsentimento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// representa a decisão do usuário sobre uma finalidade de tratamento de dados
public record ConsentimentoRequestDTO(

        // identifica a finalidade do consentimento
        @NotNull(message = "A finalidade do consentimento é obrigatória.")
        FinalidadeConsentimento finalidade,

        // identifica a versão do termo apresentado ao usuário
        @NotBlank(message = "A versão do termo é obrigatória.")
        @Size(
                max = 30,
                message = "A versão do termo deve possuir até 30 caracteres."
        )
        String versaoTermo,

        // representa a decisão explícita do usuário
        @NotNull(message = "A decisão sobre o consentimento é obrigatória.")
        Boolean concedido,

        // identifica o canal de origem da decisão
        @NotNull(message = "A origem do consentimento é obrigatória.")
        CanalPedido origem

) {
}