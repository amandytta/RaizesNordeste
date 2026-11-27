package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.enums.StatusPedido;

import jakarta.validation.constraints.NotNull;

// representa a solicitação de atualização do status de um pedido
public record PedidoStatusRequestDTO(

        // identifica o novo status solicitado
        @NotNull(message = "O status do pedido é obrigatório.")
        StatusPedido status

) {
}