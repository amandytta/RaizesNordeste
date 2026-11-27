package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.enums.CanalPedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

// representa os dados necessários para cadastrar um novo pedido
public record PedidoRequestDTO(

        // identifica a unidade responsável pelo atendimento
        @NotNull(message = "O identificador da unidade é obrigatório.")
        @Positive(message = "O identificador da unidade deve ser positivo.")
        Long unidadeId,

        // identifica o canal utilizado para realizar o pedido
        @NotNull(message = "O canal do pedido é obrigatório.")
        CanalPedido canalPedido,

        // exige pelo menos um item e limita o tamanho da requisição
        // valida individualmente todos os itens recebidos
        @NotEmpty(message = "O pedido deve possuir pelo menos um item.")
        @Size(max = 100, message = "O pedido deve possuir no máximo 100 itens.")
        List<
                @NotNull(message = "O item do pedido não pode ser nulo.")
                @Valid ItemPedidoRequestDTO
                > itens

) {
}