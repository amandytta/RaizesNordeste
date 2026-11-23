package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.entity.Categoria;

public record CategoriaResponseDTO(
        Long id,
        String nome,
        String descricao,
        Boolean ativa
) {

    public static CategoriaResponseDTO fromEntity(Categoria categoria) {
        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao(),
                categoria.getAtiva()
        );
    }
}