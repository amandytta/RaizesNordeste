package br.com.raizesnordeste.raizesnordeste.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UnidadeRequestDTOTest {

    @Test
    void naoDeveAceitarCnpjComQuantidadeIncorretaDeDigitos() {

        UnidadeRequestDTO dto = new UnidadeRequestDTO(
                "Raizes do Nordeste - Centro",
                "12345",
                "21999999999",
                "centro@raizes.local",
                "20040002",
                "Rua do Comercio",
                "100",
                "Loja 1",
                "Centro",
                "Rio de Janeiro",
                "RJ"
        );

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            var violacoes = validator.validate(dto);

            assertTrue(
                    violacoes.stream().anyMatch(
                            violacao -> violacao
                                    .getPropertyPath()
                                    .toString()
                                    .equals("cnpj")
                    )
            );
        }
    }

    @Test
    void naoDeveAceitarCamposObrigatoriosVazios() {

        UnidadeRequestDTO dto = new UnidadeRequestDTO(
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        );

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            var violacoes = validator.validate(dto);

            assertTrue(
                    violacoes.stream().anyMatch(
                            violacao -> violacao
                                    .getPropertyPath()
                                    .toString()
                                    .equals("nome")
                    )
            );

            assertTrue(
                    violacoes.stream().anyMatch(
                            violacao -> violacao
                                    .getPropertyPath()
                                    .toString()
                                    .equals("cnpj")
                    )
            );
        }
    }

    @Test
    void deveAceitarUnidadeComDadosValidos() {

        UnidadeRequestDTO dto = new UnidadeRequestDTO(
                "Raizes do Nordeste - Centro",
                "12345678000195",
                "21999999999",
                "centro@raizes.local",
                "20040002",
                "Rua do Comercio",
                "100",
                "Loja 1",
                "Centro",
                "Rio de Janeiro",
                "RJ"
        );

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            var violacoes = validator.validate(dto);

            assertTrue(violacoes.isEmpty());
        }
    }

    @Test
    void deveAceitarEmailComFormatoValido() {

        UnidadeRequestDTO dto = new UnidadeRequestDTO(
                "Raizes do Nordeste - Centro",
                "12345678000195",
                "21999999999",
                "contato@raizesnordeste.com.br",
                "20040002",
                "Rua do Comercio",
                "100",
                "Loja 1",
                "Centro",
                "Rio de Janeiro",
                "RJ"
        );

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            var violacoes = validator.validate(dto);

            assertTrue(
                    violacoes.stream().noneMatch(
                            violacao -> violacao
                                    .getPropertyPath()
                                    .toString()
                                    .equals("email")
                    )
            );
        }
    }
}