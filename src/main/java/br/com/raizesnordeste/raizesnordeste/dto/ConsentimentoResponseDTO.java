package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.enums.CanalPedido;
import br.com.raizesnordeste.raizesnordeste.enums.FinalidadeConsentimento;

import java.time.LocalDateTime;

// representa um registro do histórico de consentimentos
public record ConsentimentoResponseDTO(

        // identificador único do registro
        Long id,

        // identificador do usuário responsável pela decisão
        Long usuarioId,

        // finalidade do tratamento de dados
        FinalidadeConsentimento finalidade,

        // versão do termo associada à decisão
        String versaoTermo,

        // indica se o consentimento foi concedido
        Boolean concedido,

        // data e horário do registro da decisão
        LocalDateTime dataRegistro,

        // canal de origem do consentimento
        CanalPedido origem

) {
}