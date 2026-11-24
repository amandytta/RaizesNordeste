package br.com.raizesnordeste.raizesnordeste.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProdutoUnidadeRequestDTO(

        @NotNull(message = "O produto é obrigatório.")
        @Positive(message = "O ID do produto deve ser positivo.")
        Long produtoId,

        @NotNull(message = "A unidade é obrigatória.")
        @Positive(message = "O ID da unidade deve ser positivo.")
        Long unidadeId,

        @NotNull(message = "O preço é obrigatório.")
        @DecimalMin(
                value = "0.01",
                message = "O preço deve ser maior que zero."
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "O preço deve possuir até 8 dígitos inteiros e 2 casas decimais."
        )
        BigDecimal preco,

        @NotNull(message = "A disponibilidade é obrigatória.")
        Boolean disponivel

) {
}