package br.com.raizesnordeste.raizesnordeste.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProdutoRequestDTO(

        @NotNull(message = "A categoria é obrigatória.")
        @Positive(message = "O ID da categoria deve ser positivo.")
        Long categoriaId,

        @NotBlank(message = "O nome do produto é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @Size(max = 500, message = "A URL da imagem deve ter no máximo 500 caracteres.")
        String imagemUrl

) {
}