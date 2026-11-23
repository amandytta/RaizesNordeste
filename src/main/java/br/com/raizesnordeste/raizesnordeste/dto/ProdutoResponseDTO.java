package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.entity.Produto;

public record ProdutoResponseDTO(

        Long id,
        Long categoriaId,
        String nome,
        String descricao,
        String imagemUrl,
        Boolean ativo

) {

    public static ProdutoResponseDTO fromEntity(Produto produto) {

        return new ProdutoResponseDTO(
                produto.getId(),
                produto.getCategoria().getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getImagemUrl(),
                produto.getAtivo()
        );
    }
}