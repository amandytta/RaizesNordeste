package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.enums.MetodoPagamento;
import br.com.raizesnordeste.raizesnordeste.enums.ResultadoPagamentoMock;

import jakarta.validation.constraints.NotNull;

// representa os dados necessários para solicitar um pagamento simulado
public record PagamentoRequestDTO(

        // identifica o método escolhido para realizar o pagamento
        @NotNull(message = "O método de pagamento é obrigatório.")
        MetodoPagamento metodo,

        // identifica o resultado desejado na simulação acadêmica
        @NotNull(message = "O resultado da simulação é obrigatório.")
        ResultadoPagamentoMock resultadoSimulado

) {
}