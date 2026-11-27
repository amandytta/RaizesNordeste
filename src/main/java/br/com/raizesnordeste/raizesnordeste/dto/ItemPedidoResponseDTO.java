package br.com.raizesnordeste.raizesnordeste.dto;

import java.math.BigDecimal;

// representa os dados de um item retornados pela API
public record ItemPedidoResponseDTO(

        // identificador do item do pedido
        Long id,

        // identificador da associação entre produto e unidade
        Long produtoUnidadeId,

        // nome do produto registrado no momento da compra
        String nomeProduto,

        // quantidade adquirida
        Integer quantidade,

        // preço unitário registrado no momento da compra
        BigDecimal precoUnitario,

        // valor total correspondente ao item
        BigDecimal subtotal

) {
}