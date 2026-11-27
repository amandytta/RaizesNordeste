package br.com.raizesnordeste.raizesnordeste.gateway;

import br.com.raizesnordeste.raizesnordeste.dto.GatewayPagamentoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.enums.MetodoPagamento;
import br.com.raizesnordeste.raizesnordeste.enums.ResultadoPagamentoMock;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// simula a comunicação com um provedor externo de pagamentos
@Component
public class GatewayPagamentoMock {

    // processa uma transação financeira exclusivamente simulada
    public GatewayPagamentoResponseDTO processar(
            MetodoPagamento metodo,
            BigDecimal valor,
            ResultadoPagamentoMock resultadoSimulado
    ) {

        // valida o método de pagamento recebido
        if (metodo == null) {
            throw new IllegalArgumentException(
                    "O método de pagamento é obrigatório."
            );
        }

        // garante que o valor da transação seja positivo
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    "O valor do pagamento deve ser maior que zero."
            );
        }

        // valida o resultado solicitado para a simulação
        if (resultadoSimulado == null) {
            throw new IllegalArgumentException(
                    "O resultado da simulação é obrigatório."
            );
        }

        // gera um código único para identificar a transação simulada
        String codigoTransacao = "MOCK-" + UUID.randomUUID();

        // identifica o resultado do processamento
        StatusPagamento status = switch (resultadoSimulado) {
            case APROVADO -> StatusPagamento.APROVADO;
            case RECUSADO -> StatusPagamento.RECUSADO;
        };

        // define a mensagem correspondente ao resultado
        String mensagemRetorno = switch (resultadoSimulado) {
            case APROVADO -> "Pagamento simulado aprovado.";
            case RECUSADO -> "Pagamento simulado recusado.";
        };

        // registra o momento em que a transação foi processada
        LocalDateTime dataProcessamento = LocalDateTime.now();

        // retorna o resultado do processamento ao serviço responsável
        return new GatewayPagamentoResponseDTO(
                codigoTransacao,
                status,
                mensagemRetorno,
                dataProcessamento
        );
    }
}