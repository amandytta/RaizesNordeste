package br.com.raizesnordeste.raizesnordeste.dto;

import java.time.LocalDateTime;
import java.util.Map;

// representa os dados de auditoria disponibilizados pela API
public record AuditoriaResponseDTO(

        // identificador do evento
        Long id,

        // identificador do usuário responsável pela operação
        Long usuarioId,

        // ação realizada
        String acao,

        // entidade afetada
        String entidade,

        // identificador do registro afetado
        Long registroId,

        // informações complementares da operação
        Map<String, Object> detalhes,

        // data e hora do evento
        LocalDateTime dataEvento

) {
}