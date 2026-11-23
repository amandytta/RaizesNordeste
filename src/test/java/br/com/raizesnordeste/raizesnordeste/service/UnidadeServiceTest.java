package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.UnidadeRequestDTO;
import br.com.raizesnordeste.raizesnordeste.repository.UnidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import br.com.raizesnordeste.raizesnordeste.entity.Unidade;
import java.util.Optional;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UnidadeServiceTest {

    private UnidadeRepository unidadeRepository;
    private UnidadeService unidadeService;

    @BeforeEach
    void preparar() {
        unidadeRepository = Mockito.mock(UnidadeRepository.class);
        unidadeService = new UnidadeService(unidadeRepository);
    }

    @Test
    void naoDeveCadastrarUnidadeComCnpjDuplicado() {

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

        when(unidadeRepository.existsByCnpj(dto.cnpj()))
                .thenReturn(true);

        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> unidadeService.cadastrar(dto)
        );

        assertEquals(
                "Já existe uma unidade cadastrada com este CNPJ.",
                excecao.getMessage()
        );

        verify(unidadeRepository, never()).save(any());
    }

    @Test
    void deveCadastrarUnidadeComCnpjDisponivel() {

        UnidadeRequestDTO dto = new UnidadeRequestDTO(
                "Raizes do Nordeste - Copacabana",
                "98765432000110",
                "21988888888",
                "copacabana@raizes.local",
                "22050002",
                "Avenida Atlantica",
                "500",
                "Loja 2",
                "Copacabana",
                "Rio de Janeiro",
                "RJ"
        );

        when(unidadeRepository.existsByCnpj(dto.cnpj()))
                .thenReturn(false);

        when(unidadeRepository.save(any(Unidade.class)))
                .thenAnswer(invocacao -> {
                    Unidade unidade = invocacao.getArgument(0);
                    return unidade;
                });

        var resposta = unidadeService.cadastrar(dto);

        assertEquals(
                "Raizes do Nordeste - Copacabana",
                resposta.nome()
        );

        assertEquals(
                "98765432000110",
                resposta.cnpj()
        );

        verify(unidadeRepository).save(any(Unidade.class));
    }

    @Test
    void deveBuscarUnidadePorIdQuandoExistir() {

        Unidade unidade = new Unidade();

        unidade.setNome("Raizes do Nordeste - Centro");
        unidade.setCnpj("12345678000195");
        unidade.setCidade("Rio de Janeiro");
        unidade.setEstado("RJ");

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        var resposta = unidadeService.buscarPorId(1L);

        assertEquals(
                "Raizes do Nordeste - Centro",
                resposta.nome()
        );

        assertEquals(
                "12345678000195",
                resposta.cnpj()
        );

        assertEquals(
                "Rio de Janeiro",
                resposta.cidade()
        );

        verify(unidadeRepository).findById(1L);
    }

    @Test
    void naoDeveBuscarUnidadeQuandoIdNaoExistir() {

        when(unidadeRepository.findById(999L))
                .thenReturn(Optional.empty());

        NoSuchElementException excecao = assertThrows(
                NoSuchElementException.class,
                () -> unidadeService.buscarPorId(999L)
        );

        assertEquals(
                "Unidade não encontrada: 999",
                excecao.getMessage()
        );

        verify(unidadeRepository).findById(999L);
    }

    @Test
    void deveListarTodasAsUnidadesCadastradas() {

        Unidade unidade1 = new Unidade();
        unidade1.setNome("Raizes do Nordeste - Centro");
        unidade1.setCnpj("12345678000195");
        unidade1.setCidade("Rio de Janeiro");
        unidade1.setEstado("RJ");

        Unidade unidade2 = new Unidade();
        unidade2.setNome("Raizes do Nordeste - Copacabana");
        unidade2.setCnpj("98765432000110");
        unidade2.setCidade("Rio de Janeiro");
        unidade2.setEstado("RJ");

        when(unidadeRepository.findAll())
                .thenReturn(List.of(unidade1, unidade2));

        var resposta = unidadeService.listarTodas();

        assertEquals(2, resposta.size());

        assertEquals(
                "Raizes do Nordeste - Centro",
                resposta.get(0).nome()
        );

        assertEquals(
                "Raizes do Nordeste - Copacabana",
                resposta.get(1).nome()
        );

        verify(unidadeRepository).findAll();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistiremUnidades() {

        when(unidadeRepository.findAll())
                .thenReturn(List.of());

        var resposta = unidadeService.listarTodas();

        assertEquals(0, resposta.size());

        verify(unidadeRepository).findAll();
    }
}