package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.enums.FinalidadeConsentimento;

import java.time.LocalDateTime;

// representa a situação atual do consentimento para uma finalidade
public record ConsentimentoAtualResponseDTO(

        // finalidade do tratamento de dados
        FinalidadeConsentimento finalidade,

        // indica se existe uma decisão anterior registrada
        boolean possuiRegistro,

        // indica se existe consentimento válido para a versão vigente
        boolean concedido,

        // versão vigente do termo definida pelo backend
        String versaoTermoAtual,

        // versão do termo associada à última decisão
        String versaoTermoRegistrada,

        // data e horário da última decisão registrada
        LocalDateTime dataRegistro

) {
}