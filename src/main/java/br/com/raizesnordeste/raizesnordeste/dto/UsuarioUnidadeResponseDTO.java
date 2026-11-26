package br.com.raizesnordeste.raizesnordeste.dto;

import java.time.LocalDateTime;

// representa os dados retornados sobre o vínculo entre usuário e unidade
public record UsuarioUnidadeResponseDTO(

        // identificador do usuário vinculado
        Long usuarioId,

        // nome do usuário vinculado
        String usuarioNome,

        // identificador da unidade vinculada
        Long unidadeId,

        // nome da unidade vinculada
        String unidadeNome,

        // indica se o vínculo está ativo
        Boolean ativo,

        // data de criação do vínculo
        LocalDateTime dataVinculo

) {

    // evita a exposição de dados pessoais em logs
    @Override
    public String toString() {
        return "UsuarioUnidadeResponseDTO[dados protegidos]";
    }
}