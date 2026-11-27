package br.com.raizesnordeste.raizesnordeste.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// representa os dados necessários para incluir um item no pedido
public record ItemPedidoRequestDTO(

        // identifica o produto comercializado na unidade
        @NotNull(message = "O identificador do produto na unidade é obrigatório.")
        @Positive(message = "O identificador do produto na unidade deve ser positivo.")
        Long produtoUnidadeId,

        // informa a quantidade solicitada do produto
        @NotNull(message = "A quantidade é obrigatória.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        Integer quantidade

) {
}