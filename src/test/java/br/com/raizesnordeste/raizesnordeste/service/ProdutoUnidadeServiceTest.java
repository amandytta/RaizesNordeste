package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.repository.ProdutoRepository;
import br.com.raizesnordeste.raizesnordeste.repository.ProdutoUnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.dto.ProdutoUnidadeRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.ProdutoUnidadeResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Produto;
import br.com.raizesnordeste.raizesnordeste.entity.ProdutoUnidade;
import br.com.raizesnordeste.raizesnordeste.entity.Unidade;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class)
class ProdutoUnidadeServiceTest {

    @Test
    void deveCadastrarProdutoUnidadeComSucesso() {

        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");
        produto.setAtivo(true);

        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");
        unidade.setAtiva(true);

        ProdutoUnidadeRequestDTO dto =
                new ProdutoUnidadeRequestDTO(
                        1L,
                        1L,
                        new BigDecimal("12.90"),
                        true
                );

        when(produtoRepository.findById(1L))
                .thenReturn(Optional.of(produto));

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        when(produtoUnidadeRepository
                .existsByProdutoIdAndUnidadeId(1L, 1L))
                .thenReturn(false);

        when(produtoUnidadeRepository.save(any(ProdutoUnidade.class)))
                .thenAnswer(invocation -> {
                    ProdutoUnidade associacao = invocation.getArgument(0);
                    ReflectionTestUtils.setField(associacao, "id", 1L);
                    return associacao;
                });

        ProdutoUnidadeResponseDTO resposta =
                produtoUnidadeService.cadastrar(dto);

        assertNotNull(resposta);
        assertEquals(1L, resposta.id());
        assertEquals(1L, resposta.produtoId());
        assertEquals("Cuscuz nordestino", resposta.produtoNome());
        assertEquals(1L, resposta.unidadeId());
        assertEquals("Raizes do Nordeste - Centro", resposta.unidadeNome());
        assertEquals(new BigDecimal("12.90"), resposta.preco());
        assertEquals(0, resposta.estoque());
        assertTrue(resposta.disponivel());

        verify(produtoUnidadeRepository, times(1))
                .save(any(ProdutoUnidade.class));
    }

    @Mock
    private ProdutoUnidadeRepository produtoUnidadeRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private ProdutoUnidadeService produtoUnidadeService;

    @Test
    void deveImpedirCadastroDeAssociacaoDuplicada() {

        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setAtivo(true);

        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setAtiva(true);

        ProdutoUnidadeRequestDTO dto =
                new ProdutoUnidadeRequestDTO(
                        1L,
                        1L,
                        new BigDecimal("12.90"),
                        true
                );

        when(produtoRepository.findById(1L))
                .thenReturn(Optional.of(produto));

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        when(produtoUnidadeRepository
                .existsByProdutoIdAndUnidadeId(1L, 1L))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> produtoUnidadeService.cadastrar(dto)
        );

        assertEquals(
                "Este produto já está cadastrado nesta unidade.",
                exception.getMessage()
        );

        verify(produtoUnidadeRepository, never())
                .save(any(ProdutoUnidade.class));
    }

    @Test
    void deveImpedirCadastroComProdutoInexistente() {

        ProdutoUnidadeRequestDTO dto =
                new ProdutoUnidadeRequestDTO(
                        99999L,
                        1L,
                        new BigDecimal("12.90"),
                        true
                );

        when(produtoRepository.findById(99999L))
                .thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> produtoUnidadeService.cadastrar(dto)
        );

        assertEquals(
                "Produto não encontrado: 99999",
                exception.getMessage()
        );

        verifyNoInteractions(unidadeRepository);

        verify(produtoUnidadeRepository, never())
                .save(any(ProdutoUnidade.class));
    }

    @Test
    void deveImpedirCadastroComUnidadeInexistente() {

        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setAtivo(true);

        ProdutoUnidadeRequestDTO dto =
                new ProdutoUnidadeRequestDTO(
                        1L,
                        99999L,
                        new BigDecimal("12.90"),
                        true
                );

        when(produtoRepository.findById(1L))
                .thenReturn(Optional.of(produto));

        when(unidadeRepository.findById(99999L))
                .thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> produtoUnidadeService.cadastrar(dto)
        );

        assertEquals(
                "Unidade não encontrada: 99999",
                exception.getMessage()
        );

        verifyNoInteractions(produtoUnidadeRepository);
    }

    @Test
    void deveImpedirCadastroComProdutoInativo() {

        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setAtivo(false);

        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setAtiva(true);

        ProdutoUnidadeRequestDTO dto =
                new ProdutoUnidadeRequestDTO(
                        1L,
                        1L,
                        new BigDecimal("12.90"),
                        true
                );

        when(produtoRepository.findById(1L))
                .thenReturn(Optional.of(produto));

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> produtoUnidadeService.cadastrar(dto)
        );

        assertEquals(
                "Não é possível associar um produto inativo.",
                exception.getMessage()
        );

        verifyNoInteractions(produtoUnidadeRepository);
    }

    @Test
    void deveImpedirCadastroComUnidadeInativa() {

        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setAtivo(true);

        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setAtiva(false);

        ProdutoUnidadeRequestDTO dto =
                new ProdutoUnidadeRequestDTO(
                        1L,
                        1L,
                        new BigDecimal("12.90"),
                        true
                );

        when(produtoRepository.findById(1L))
                .thenReturn(Optional.of(produto));

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> produtoUnidadeService.cadastrar(dto)
        );

        assertEquals(
                "Não é possível associar produtos a uma unidade inativa.",
                exception.getMessage()
        );

        verifyNoInteractions(produtoUnidadeRepository);
    }

    @Test
    void deveListarTodasAsAssociacoes() {

        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");

        ProdutoUnidade associacao = new ProdutoUnidade();
        ReflectionTestUtils.setField(associacao, "id", 1L);
        associacao.setProduto(produto);
        associacao.setUnidade(unidade);
        associacao.setPreco(new BigDecimal("12.90"));
        associacao.setEstoque(10);
        associacao.setDisponivel(true);

        when(produtoUnidadeRepository.findAll())
                .thenReturn(List.of(associacao));

        List<ProdutoUnidadeResponseDTO> resposta =
                produtoUnidadeService.listarTodos();

        assertNotNull(resposta);
        assertEquals(1, resposta.size());
        assertEquals(1L, resposta.get(0).id());
        assertEquals("Cuscuz nordestino", resposta.get(0).produtoNome());
        assertEquals("Raizes do Nordeste - Centro", resposta.get(0).unidadeNome());
        assertEquals(new BigDecimal("12.90"), resposta.get(0).preco());
        assertEquals(10, resposta.get(0).estoque());
        assertTrue(resposta.get(0).disponivel());

        verify(produtoUnidadeRepository, times(1)).findAll();
    }

    @Test
    void deveListarCardapioApenasComProdutosAtivos() {

        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");
        unidade.setAtiva(true);

        Produto produtoAtivo = new Produto();
        ReflectionTestUtils.setField(produtoAtivo, "id", 1L);
        produtoAtivo.setNome("Cuscuz nordestino");
        produtoAtivo.setAtivo(true);

        Produto produtoInativo = new Produto();
        ReflectionTestUtils.setField(produtoInativo, "id", 2L);
        produtoInativo.setNome("Tapioca");
        produtoInativo.setAtivo(false);

        ProdutoUnidade associacaoAtiva = new ProdutoUnidade();
        ReflectionTestUtils.setField(associacaoAtiva, "id", 1L);
        associacaoAtiva.setProduto(produtoAtivo);
        associacaoAtiva.setUnidade(unidade);
        associacaoAtiva.setPreco(new BigDecimal("12.90"));
        associacaoAtiva.setEstoque(10);
        associacaoAtiva.setDisponivel(true);

        ProdutoUnidade associacaoInativa = new ProdutoUnidade();
        ReflectionTestUtils.setField(associacaoInativa, "id", 2L);
        associacaoInativa.setProduto(produtoInativo);
        associacaoInativa.setUnidade(unidade);
        associacaoInativa.setPreco(new BigDecimal("8.90"));
        associacaoInativa.setEstoque(5);
        associacaoInativa.setDisponivel(true);

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        when(produtoUnidadeRepository
                .findByUnidadeIdAndDisponivelTrue(1L))
                .thenReturn(List.of(
                        associacaoAtiva,
                        associacaoInativa
                ));

        List<ProdutoUnidadeResponseDTO> resposta =
                produtoUnidadeService.listarCardapioPorUnidade(1L);

        assertNotNull(resposta);
        assertEquals(1, resposta.size());
        assertEquals(1L, resposta.get(0).produtoId());
        assertEquals("Cuscuz nordestino", resposta.get(0).produtoNome());
        assertEquals(1L, resposta.get(0).unidadeId());
        assertTrue(resposta.get(0).disponivel());

        verify(unidadeRepository, times(1)).findById(1L);

        verify(produtoUnidadeRepository, times(1))
                .findByUnidadeIdAndDisponivelTrue(1L);
    }
}