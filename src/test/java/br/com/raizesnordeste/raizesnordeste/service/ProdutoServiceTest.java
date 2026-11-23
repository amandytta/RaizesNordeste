package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.repository.CategoriaRepository;
import br.com.raizesnordeste.raizesnordeste.repository.ProdutoRepository;

import br.com.raizesnordeste.raizesnordeste.dto.ProdutoRequestDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Categoria;
import br.com.raizesnordeste.raizesnordeste.dto.ProdutoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Produto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ProdutoService produtoService;

    @BeforeEach
    void preparar() {
        // Preparação dos testes.
    }

    @Test
    void deveImpedirCadastroDeProdutoDuplicado() {

        // Arrange: preparar os dados do teste
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
                1L,
                "Cuscuz nordestino",
                "Cuscuz de milho tradicional",
                null
        );

        Categoria categoria = new Categoria();

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(produtoRepository.existsByNomeAndCategoriaId(
                "Cuscuz nordestino", 1L
        )).thenReturn(true);

        // Act e Assert: executar e verificar a exceção
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> produtoService.cadastrar(dto)
        );

        assertEquals(
                "Já existe um produto com este nome nesta categoria.",
                excecao.getMessage()
        );

        // Verificar que o produto não foi salvo
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void deveCadastrarProdutoComSucesso() {

        // Arrange: preparar os dados
        ProdutoRequestDTO dto = new ProdutoRequestDTO(
                1L,
                "Tapioca nordestina",
                "Tapioca tradicional recheada",
                null
        );

        Categoria categoria = new Categoria();

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(produtoRepository.existsByNomeAndCategoriaId(
                "Tapioca nordestina", 1L
        )).thenReturn(false);

        when(produtoRepository.save(any(Produto.class)))
                .thenAnswer(invocacao ->
                        invocacao.getArgument(0)
                );

        // Act: executar o cadastro
        ProdutoResponseDTO resposta = produtoService.cadastrar(dto);

        // Assert: verificar os resultados
        assertNotNull(resposta);

        assertEquals("Tapioca nordestina", resposta.nome());

        assertEquals(
                "Tapioca tradicional recheada",
                resposta.descricao()
        );

        assertTrue(resposta.ativo());

        ArgumentCaptor<Produto> captor =
                ArgumentCaptor.forClass(Produto.class);

        verify(produtoRepository).save(captor.capture());

        Produto produtoSalvo = captor.getValue();

        assertEquals("Tapioca nordestina", produtoSalvo.getNome());

        assertEquals(categoria, produtoSalvo.getCategoria());
    }

    @Test
    void deveBuscarProdutoPorIdComSucesso() {

        // Arrange: preparar uma categoria
        Categoria categoria = new Categoria();

        // Simular o ID da categoria
        org.springframework.test.util.ReflectionTestUtils.setField(
                categoria, "id", 1L
        );

        // Preparar um produto existente
        Produto produto = new Produto();

        org.springframework.test.util.ReflectionTestUtils.setField(
                produto, "id", 10L
        );

        produto.setCategoria(categoria);
        produto.setNome("Baião de dois");
        produto.setDescricao("Prato típico nordestino");

        // Simular a consulta ao Repository
        when(produtoRepository.findById(10L))
                .thenReturn(Optional.of(produto));

        // Act: buscar o produto
        ProdutoResponseDTO resposta = produtoService.buscarPorId(10L);

        // Assert: verificar os dados retornados
        assertNotNull(resposta);
        assertEquals(10L, resposta.id());
        assertEquals(1L, resposta.categoriaId());
        assertEquals("Baião de dois", resposta.nome());
        assertEquals("Prato típico nordestino", resposta.descricao());
        assertTrue(resposta.ativo());

        // Confirmar que o Repository foi consultado
        verify(produtoRepository).findById(10L);
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoExiste() {

        // Arrange: simular um produto inexistente
        when(produtoRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act e Assert: verificar a exceção
        NoSuchElementException excecao = assertThrows(
                NoSuchElementException.class,
                () -> produtoService.buscarPorId(999L)
        );

        assertEquals(
                "Produto não encontrado: 999",
                excecao.getMessage()
        );

        // Confirmar que o Repository foi consultado
        verify(produtoRepository).findById(999L);
    }

    @Test
    void deveListarProdutosCadastrados() {

        // Arrange: preparar a categoria
        Categoria categoria = new Categoria();

        org.springframework.test.util.ReflectionTestUtils.setField(
                categoria, "id", 1L
        );

        // Preparar o primeiro produto
        Produto produto1 = new Produto();

        org.springframework.test.util.ReflectionTestUtils.setField(
                produto1, "id", 1L
        );

        produto1.setCategoria(categoria);
        produto1.setNome("Cuscuz nordestino");
        produto1.setDescricao("Cuscuz de milho tradicional");

        // Preparar o segundo produto
        Produto produto2 = new Produto();

        org.springframework.test.util.ReflectionTestUtils.setField(
                produto2, "id", 2L
        );

        produto2.setCategoria(categoria);
        produto2.setNome("Tapioca nordestina");
        produto2.setDescricao("Tapioca tradicional recheada");

        // Simular os produtos encontrados
        when(produtoRepository.findAll())
                .thenReturn(List.of(produto1, produto2));

        // Act: executar a listagem
        List<ProdutoResponseDTO> resposta =
                produtoService.listarTodos();

        // Assert: verificar os resultados
        assertNotNull(resposta);
        assertEquals(2, resposta.size());

        assertEquals("Cuscuz nordestino", resposta.get(0).nome());
        assertEquals("Tapioca nordestina", resposta.get(1).nome());

        assertEquals(1L, resposta.get(0).categoriaId());
        assertEquals(1L, resposta.get(1).categoriaId());

        // Confirmar a consulta ao Repository
        verify(produtoRepository).findAll();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistemProdutos() {

        // Arrange: simular um banco sem produtos
        when(produtoRepository.findAll())
                .thenReturn(List.of());

        // Act: executar a listagem
        List<ProdutoResponseDTO> resposta =
                produtoService.listarTodos();

        // Assert: verificar o resultado
        assertNotNull(resposta);
        assertTrue(resposta.isEmpty());
        assertEquals(0, resposta.size());

        // Confirmar que o Repository foi consultado
        verify(produtoRepository).findAll();
    }
}