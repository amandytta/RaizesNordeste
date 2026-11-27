package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.enums.CanalPedido;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// representa os dados completos de um pedido retornados pela API
public record PedidoResponseDTO(

        // identificador único do pedido
        Long id,

        // identificação da unidade responsável pelo atendimento
        Long unidadeId,

        // nome da unidade responsável pelo atendimento
        String unidadeNome,

        // identificador do usuário associado ao pedido
        Long usuarioId,

        // canal utilizado para realizar o pedido
        CanalPedido canalPedido,

        // situação atual do pedido
        StatusPedido status,

        // valor total calculado pelo backend
        BigDecimal valorTotal,

        // lista dos itens associados ao pedido
        List<ItemPedidoResponseDTO> itens,

        // data e horário de criação
        LocalDateTime dataCriacao,

        // data e horário da última atualização
        LocalDateTime dataAtualizacao

) {
}