package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.CategoriaRequestDTO;
import br.com.raizesnordeste.raizesnordeste.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import br.com.raizesnordeste.raizesnordeste.entity.Categoria;
import java.util.Optional;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoriaServiceTest {

    private CategoriaRepository categoriaRepository;
    private CategoriaService categoriaService;

    @BeforeEach
    void preparar() {
        categoriaRepository = Mockito.mock(CategoriaRepository.class);
        categoriaService = new CategoriaService(categoriaRepository);
    }

    @Test
    void naoDeveCadastrarCategoriaComNomeDuplicado() {

        CategoriaRequestDTO dto = new CategoriaRequestDTO(
                "Pratos típicos",
                "Pratos tradicionais da culinária nordestina"
        );

        when(categoriaRepository.existsByNome(dto.nome()))
                .thenReturn(true);

        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.cadastrar(dto)
        );

        assertEquals(
                "Já existe uma categoria cadastrada com este nome.",
                excecao.getMessage()
        );

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void deveCadastrarCategoriaComNomeDisponivel() {

        CategoriaRequestDTO dto = new CategoriaRequestDTO(
                "Sobremesas",
                "Doces e sobremesas tradicionais do Nordeste"
        );

        when(categoriaRepository.existsByNome(dto.nome()))
                .thenReturn(false);

        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = categoriaService.cadastrar(dto);

        assertEquals("Sobremesas", resposta.nome());

        assertEquals(
                "Doces e sobremesas tradicionais do Nordeste",
                resposta.descricao()
        );

        assertEquals(true, resposta.ativa());

        verify(categoriaRepository).existsByNome("Sobremesas");
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void deveBuscarCategoriaPorId() {

        Categoria categoria = new Categoria();
        categoria.setNome("Bebidas");
        categoria.setDescricao("Bebidas tradicionais do Nordeste");
        categoria.setAtiva(true);

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        var resposta = categoriaService.buscarPorId(1L);

        assertEquals("Bebidas", resposta.nome());

        assertEquals(
                "Bebidas tradicionais do Nordeste",
                resposta.descricao()
        );

        assertTrue(resposta.ativa());

        verify(categoriaRepository).findById(1L);
    }

    @Test
    void deveLancarExcecaoQuandoCategoriaNaoExiste() {

        when(categoriaRepository.findById(999L))
                .thenReturn(Optional.empty());

        NoSuchElementException excecao = assertThrows(
                NoSuchElementException.class,
                () -> categoriaService.buscarPorId(999L)
        );

        assertEquals(
                "Categoria não encontrada: 999",
                excecao.getMessage()
        );

        verify(categoriaRepository).findById(999L);
    }

    @Test
    void deveListarTodasAsCategorias() {

        Categoria categoria1 = new Categoria();
        categoria1.setNome("Pratos típicos");
        categoria1.setDescricao("Pratos tradicionais do Nordeste");
        categoria1.setAtiva(true);

        Categoria categoria2 = new Categoria();
        categoria2.setNome("Sobremesas");
        categoria2.setDescricao("Doces tradicionais do Nordeste");
        categoria2.setAtiva(true);

        when(categoriaRepository.findAll())
                .thenReturn(List.of(categoria1, categoria2));

        var resposta = categoriaService.listarTodas();

        assertEquals(2, resposta.size());

        assertEquals("Pratos típicos", resposta.get(0).nome());
        assertEquals("Sobremesas", resposta.get(1).nome());

        verify(categoriaRepository).findAll();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistemCategorias() {

        when(categoriaRepository.findAll())
                .thenReturn(List.of());

        var resposta = categoriaService.listarTodas();

        assertNotNull(resposta);

        assertTrue(resposta.isEmpty());

        verify(categoriaRepository).findAll();
    }
}