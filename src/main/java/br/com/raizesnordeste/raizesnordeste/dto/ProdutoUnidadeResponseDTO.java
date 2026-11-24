package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.entity.ProdutoUnidade;

import java.math.BigDecimal;

public record ProdutoUnidadeResponseDTO(

        Long id,
        Long produtoId,
        String produtoNome,
        Long unidadeId,
        String unidadeNome,
        BigDecimal preco,
        Integer estoque,
        Boolean disponivel

) {

    public static ProdutoUnidadeResponseDTO fromEntity(
            ProdutoUnidade produtoUnidade
    ) {

        return new ProdutoUnidadeResponseDTO(
                produtoUnidade.getId(),
                produtoUnidade.getProduto().getId(),
                produtoUnidade.getProduto().getNome(),
                produtoUnidade.getUnidade().getId(),
                produtoUnidade.getUnidade().getNome(),
                produtoUnidade.getPreco(),
                produtoUnidade.getEstoque(),
                produtoUnidade.getDisponivel()
        );
    }
}