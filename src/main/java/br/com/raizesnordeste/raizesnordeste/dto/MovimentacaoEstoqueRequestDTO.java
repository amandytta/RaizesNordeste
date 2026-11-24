package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.enums.TipoMovimentacaoEstoque;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MovimentacaoEstoqueRequestDTO(

        @NotNull(message = "O produto da unidade é obrigatório.")
        @Positive(message = "O ID do produto da unidade deve ser positivo.")
        Long produtoUnidadeId,

        @NotNull(message = "O tipo de movimentação é obrigatório.")
        TipoMovimentacaoEstoque tipo,

        @NotNull(message = "A quantidade é obrigatória.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        Integer quantidade,

        @NotBlank(message = "O motivo da movimentação é obrigatório.")
        @Size(max = 255, message = "O motivo deve ter no máximo 255 caracteres.")
        String motivo

) {
}