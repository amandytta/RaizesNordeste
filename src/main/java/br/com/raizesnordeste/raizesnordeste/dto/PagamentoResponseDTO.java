package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.enums.MetodoPagamento;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPagamento;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// representa os dados de uma transação retornados pela API
public record PagamentoResponseDTO(

        // identificador único do pagamento
        Long id,

        // identificador do pedido associado
        Long pedidoId,

        // método utilizado na transação
        MetodoPagamento metodo,

        // situação do pagamento
        StatusPagamento status,

        // valor da transação determinado pelo backend
        BigDecimal valor,

        // código único gerado pelo gateway mock
        String codigoTransacao,

        // mensagem informada pelo gateway
        String mensagemRetorno,

        // situação atual do pedido associado
        StatusPedido statusPedido,

        // data e horário de criação do pagamento
        LocalDateTime dataCriacao,

        // data e horário de processamento da transação
        LocalDateTime dataProcessamento

) {
}