package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.entity.MovimentacaoEstoque;
import br.com.raizesnordeste.raizesnordeste.enums.TipoMovimentacaoEstoque;

import java.time.LocalDateTime;

public record MovimentacaoEstoqueResponseDTO(

        Long id,
        Long produtoUnidadeId,
        Long produtoId,
        String produtoNome,
        Long unidadeId,
        String unidadeNome,
        Long pedidoId,
        Long usuarioId,
        TipoMovimentacaoEstoque tipo,
        Integer quantidade,
        Integer estoqueAnterior,
        Integer estoquePosterior,
        String motivo,
        LocalDateTime dataMovimentacao

) {

    public static MovimentacaoEstoqueResponseDTO fromEntity(
            MovimentacaoEstoque movimentacao
    ) {

        return new MovimentacaoEstoqueResponseDTO(
                movimentacao.getId(),
                movimentacao.getProdutoUnidade().getId(),
                movimentacao.getProdutoUnidade().getProduto().getId(),
                movimentacao.getProdutoUnidade().getProduto().getNome(),
                movimentacao.getProdutoUnidade().getUnidade().getId(),
                movimentacao.getProdutoUnidade().getUnidade().getNome(),
                movimentacao.getPedidoId(),
                movimentacao.getUsuario() != null
                        ? movimentacao.getUsuario().getId()
                        : null,
                movimentacao.getTipo(),
                movimentacao.getQuantidade(),
                movimentacao.getEstoqueAnterior(),
                movimentacao.getEstoquePosterior(),
                movimentacao.getMotivo(),
                movimentacao.getDataMovimentacao()
        );
    }
}