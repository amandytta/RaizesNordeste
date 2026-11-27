package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.enums.StatusPagamento;

import java.time.LocalDateTime;

// representa o resultado interno retornado pelo gateway de pagamento mock
public record GatewayPagamentoResponseDTO(

        // código único gerado para identificar a transação simulada
        String codigoTransacao,

        // resultado do processamento do pagamento
        StatusPagamento status,

        // mensagem explicativa retornada pelo gateway
        String mensagemRetorno,

        // data e horário do processamento
        LocalDateTime dataProcessamento

) {
}