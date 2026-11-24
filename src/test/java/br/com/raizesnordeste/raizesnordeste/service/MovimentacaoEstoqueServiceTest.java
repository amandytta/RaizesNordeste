package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.repository.MovimentacaoEstoqueRepository;
import br.com.raizesnordeste.raizesnordeste.repository.ProdutoUnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;
import br.com.raizesnordeste.raizesnordeste.dto.MovimentacaoEstoqueRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.MovimentacaoEstoqueResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.MovimentacaoEstoque;
import br.com.raizesnordeste.raizesnordeste.entity.Produto;
import br.com.raizesnordeste.raizesnordeste.entity.ProdutoUnidade;
import br.com.raizesnordeste.raizesnordeste.entity.Unidade;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.enums.TipoMovimentacaoEstoque;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// habilita a integração entre o JUnit 5 e o Mockito
@ExtendWith(MockitoExtension.class)
class MovimentacaoEstoqueServiceTest {

    // simula o repositório responsável pelo histórico de estoque
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    // simula o repositório responsável pelo estoque de cada unidade
    @Mock
    private ProdutoUnidadeRepository produtoUnidadeRepository;

    // simula o repositório responsável pela consulta dos usuários
    @Mock
    private UsuarioRepository usuarioRepository;

    // injeta os repositórios simulados no serviço que será testado
    @InjectMocks
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Test
    void deveRegistrarEntradaDeEstoqueComSucesso() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // cria o produto utilizado no teste
        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        // cria uma unidade ativa
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");
        unidade.setAtiva(true);

        // cria a associação entre produto e unidade
        ProdutoUnidade produtoUnidade = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade, "id", 1L);
        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setEstoque(20);

        // define os dados da movimentação de entrada
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.ENTRADA,
                        30,
                        "Recebimento de mercadoria"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // simula a localização do estoque pelo método com bloqueio
        when(produtoUnidadeRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(produtoUnidade));

        // simula a persistência da movimentação
        when(movimentacaoEstoqueRepository.save(
                any(MovimentacaoEstoque.class)))
                .thenAnswer(invocation -> {

                    MovimentacaoEstoque movimentacao =
                            invocation.getArgument(0);

                    // simula o ID gerado durante a persistência
                    ReflectionTestUtils.setField(
                            movimentacao, "id", 1L
                    );

                    // simula o preenchimento da data de criação
                    movimentacao.prePersist();

                    return movimentacao;
                });

        // executa o método que será testado
        MovimentacaoEstoqueResponseDTO resposta =
                movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                );

        // verifica se o saldo foi atualizado corretamente
        assertEquals(50, produtoUnidade.getEstoque());

        // verifica os dados retornados pelo serviço
        assertEquals(1L, resposta.id());
        assertEquals(TipoMovimentacaoEstoque.ENTRADA, resposta.tipo());
        assertEquals(30, resposta.quantidade());
        assertEquals(20, resposta.estoqueAnterior());
        assertEquals(50, resposta.estoquePosterior());
        assertEquals(1L, resposta.usuarioId());
        assertEquals("Recebimento de mercadoria", resposta.motivo());
        assertNotNull(resposta.dataMovimentacao());

        // captura a movimentação enviada ao repositório
        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);

        // verifica se o estoque e o histórico foram enviados para persistência
        verify(produtoUnidadeRepository).save(produtoUnidade);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        // recupera a movimentação capturada
        MovimentacaoEstoque movimentacaoSalva = captor.getValue();

        // confirma os relacionamentos e saldos da movimentação
        assertSame(usuario, movimentacaoSalva.getUsuario());
        assertSame(produtoUnidade, movimentacaoSalva.getProdutoUnidade());
        assertEquals(20, movimentacaoSalva.getEstoqueAnterior());
        assertEquals(50, movimentacaoSalva.getEstoquePosterior());
    }

    @Test
    void deveRegistrarSaidaDeEstoqueComSucesso() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // cria o produto utilizado no teste
        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        // cria uma unidade ativa
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");
        unidade.setAtiva(true);

        // cria a associação com um estoque inicial de 80 unidades
        ProdutoUnidade produtoUnidade = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade, "id", 1L);
        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setEstoque(80);

        // define os dados da movimentação de saída
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.SAIDA,
                        25,
                        "Saida de mercadoria para consumo"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // simula a localização do estoque pelo método com bloqueio
        when(produtoUnidadeRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(produtoUnidade));

        // simula a persistência da movimentação
        when(movimentacaoEstoqueRepository.save(
                any(MovimentacaoEstoque.class)))
                .thenAnswer(invocation -> {

                    MovimentacaoEstoque movimentacao =
                            invocation.getArgument(0);

                    // simula o ID gerado pelo banco de dados
                    ReflectionTestUtils.setField(
                            movimentacao, "id", 2L
                    );

                    // simula o preenchimento da data da movimentação
                    movimentacao.prePersist();

                    return movimentacao;
                });

        // executa o registro da movimentação de saída
        MovimentacaoEstoqueResponseDTO resposta =
                movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                );

        // verifica se a quantidade foi descontada corretamente
        assertEquals(55, produtoUnidade.getEstoque());

        // verifica os dados retornados pelo serviço
        assertEquals(2L, resposta.id());
        assertEquals(TipoMovimentacaoEstoque.SAIDA, resposta.tipo());
        assertEquals(25, resposta.quantidade());
        assertEquals(80, resposta.estoqueAnterior());
        assertEquals(55, resposta.estoquePosterior());
        assertEquals(1L, resposta.usuarioId());
        assertEquals("Saida de mercadoria para consumo", resposta.motivo());
        assertNotNull(resposta.dataMovimentacao());

        // captura a movimentação enviada ao repositório
        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);

        // verifica se o saldo e o histórico foram enviados para persistência
        verify(produtoUnidadeRepository).save(produtoUnidade);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        // recupera a movimentação capturada
        MovimentacaoEstoque movimentacaoSalva = captor.getValue();

        // confirma os relacionamentos e os dados do histórico
        assertSame(usuario, movimentacaoSalva.getUsuario());
        assertSame(produtoUnidade, movimentacaoSalva.getProdutoUnidade());
        assertEquals(TipoMovimentacaoEstoque.SAIDA, movimentacaoSalva.getTipo());
        assertEquals(25, movimentacaoSalva.getQuantidade());
        assertEquals(80, movimentacaoSalva.getEstoqueAnterior());
        assertEquals(55, movimentacaoSalva.getEstoquePosterior());
    }

    @Test
    void devePermitirSaidaDeTodoEstoqueDisponivel() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // cria o produto utilizado no teste
        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        // cria uma unidade ativa
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");
        unidade.setAtiva(true);

        // cria a associação com um estoque inicial de 40 unidades
        ProdutoUnidade produtoUnidade = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade, "id", 1L);
        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setEstoque(40);

        // solicita a retirada de toda a quantidade disponível
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.SAIDA,
                        40,
                        "Retirada de todo o estoque disponivel"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // simula a localização do estoque pelo método com bloqueio
        when(produtoUnidadeRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(produtoUnidade));

        // simula a persistência da movimentação
        when(movimentacaoEstoqueRepository.save(
                any(MovimentacaoEstoque.class)))
                .thenAnswer(invocation -> {

                    MovimentacaoEstoque movimentacao =
                            invocation.getArgument(0);

                    // simula o ID gerado pelo banco de dados
                    ReflectionTestUtils.setField(
                            movimentacao, "id", 3L
                    );

                    // simula o preenchimento da data da movimentação
                    movimentacao.prePersist();

                    return movimentacao;
                });

        // executa a movimentação de saída
        MovimentacaoEstoqueResponseDTO resposta =
                movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                );

        // confirma que o estoque foi zerado corretamente
        assertEquals(0, produtoUnidade.getEstoque());

        // verifica os dados retornados pelo serviço
        assertEquals(3L, resposta.id());
        assertEquals(TipoMovimentacaoEstoque.SAIDA, resposta.tipo());
        assertEquals(40, resposta.quantidade());
        assertEquals(40, resposta.estoqueAnterior());
        assertEquals(0, resposta.estoquePosterior());

        // captura o registro enviado ao repositório de movimentações
        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);

        // verifica se o saldo e o histórico foram enviados para persistência
        verify(produtoUnidadeRepository).save(produtoUnidade);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        // confirma os saldos registrados no histórico
        assertEquals(40, captor.getValue().getEstoqueAnterior());
        assertEquals(0, captor.getValue().getEstoquePosterior());
    }

    @Test
    void deveListarTodasAsMovimentacoes() {

        // cria o usuário responsável pelas movimentações
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);

        // cria o produto utilizado no teste
        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        // cria a unidade responsável pelo estoque
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");

        // cria a associação entre produto e unidade
        ProdutoUnidade produtoUnidade = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade, "id", 1L);
        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setEstoque(35);

        // cria a primeira movimentação: entrada de 50 unidades
        MovimentacaoEstoque entrada = new MovimentacaoEstoque();
        ReflectionTestUtils.setField(entrada, "id", 1L);
        entrada.setProdutoUnidade(produtoUnidade);
        entrada.setUsuario(usuario);
        entrada.setTipo(TipoMovimentacaoEstoque.ENTRADA);
        entrada.setQuantidade(50);
        entrada.setEstoqueAnterior(0);
        entrada.setEstoquePosterior(50);
        entrada.setMotivo("Recebimento de mercadoria");
        entrada.prePersist();

        // cria a segunda movimentação: saída de 15 unidades
        MovimentacaoEstoque saida = new MovimentacaoEstoque();
        ReflectionTestUtils.setField(saida, "id", 2L);
        saida.setProdutoUnidade(produtoUnidade);
        saida.setUsuario(usuario);
        saida.setTipo(TipoMovimentacaoEstoque.SAIDA);
        saida.setQuantidade(15);
        saida.setEstoqueAnterior(50);
        saida.setEstoquePosterior(35);
        saida.setMotivo("Saida de mercadoria");
        saida.prePersist();

        // simula o retorno do histórico em ordem decrescente
        when(movimentacaoEstoqueRepository
                .findAllByOrderByDataMovimentacaoDesc())
                .thenReturn(List.of(saida, entrada));

        // executa a consulta de todas as movimentações
        List<MovimentacaoEstoqueResponseDTO> resposta =
                movimentacaoEstoqueService.listarTodas();

        // confirma que as duas movimentações foram retornadas
        assertEquals(2, resposta.size());

        // verifica os dados da primeira movimentação retornada
        assertEquals(2L, resposta.get(0).id());
        assertEquals(TipoMovimentacaoEstoque.SAIDA,
                resposta.get(0).tipo());
        assertEquals(15, resposta.get(0).quantidade());
        assertEquals(35, resposta.get(0).estoquePosterior());

        // verifica os dados da segunda movimentação retornada
        assertEquals(1L, resposta.get(1).id());
        assertEquals(TipoMovimentacaoEstoque.ENTRADA,
                resposta.get(1).tipo());
        assertEquals(50, resposta.get(1).quantidade());
        assertEquals(50, resposta.get(1).estoquePosterior());

        // confirma que os relacionamentos foram convertidos corretamente
        assertEquals("Cuscuz nordestino",
                resposta.get(0).produtoNome());
        assertEquals("Raizes do Nordeste - Centro",
                resposta.get(0).unidadeNome());

        // verifica se o método correto do repositório foi utilizado
        verify(movimentacaoEstoqueRepository)
                .findAllByOrderByDataMovimentacaoDesc();
    }

    @Test
    void deveBuscarMovimentacaoPorIdComSucesso() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);

        // cria o produto utilizado no teste
        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        // cria a unidade responsável pelo estoque
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");

        // cria a associação entre produto e unidade
        ProdutoUnidade produtoUnidade = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade, "id", 1L);
        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setEstoque(70);

        // cria uma movimentação de entrada de estoque
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        ReflectionTestUtils.setField(movimentacao, "id", 5L);
        movimentacao.setProdutoUnidade(produtoUnidade);
        movimentacao.setUsuario(usuario);
        movimentacao.setTipo(TipoMovimentacaoEstoque.ENTRADA);
        movimentacao.setQuantidade(20);
        movimentacao.setEstoqueAnterior(50);
        movimentacao.setEstoquePosterior(70);
        movimentacao.setMotivo("Reposicao de mercadoria");

        // simula o preenchimento da data da movimentação
        movimentacao.prePersist();

        // simula a localização da movimentação pelo ID
        when(movimentacaoEstoqueRepository.findById(5L))
                .thenReturn(Optional.of(movimentacao));

        // executa a busca da movimentação
        MovimentacaoEstoqueResponseDTO resposta =
                movimentacaoEstoqueService.buscarPorId(5L);

        // verifica se a movimentação correta foi retornada
        assertNotNull(resposta);
        assertEquals(5L, resposta.id());

        // verifica os identificadores dos relacionamentos
        assertEquals(1L, resposta.produtoUnidadeId());
        assertEquals(1L, resposta.produtoId());
        assertEquals(1L, resposta.unidadeId());
        assertEquals(1L, resposta.usuarioId());

        // verifica os nomes do produto e da unidade
        assertEquals("Cuscuz nordestino", resposta.produtoNome());
        assertEquals("Raizes do Nordeste - Centro",
                resposta.unidadeNome());

        // verifica os dados da movimentação
        assertEquals(TipoMovimentacaoEstoque.ENTRADA,
                resposta.tipo());
        assertEquals(20, resposta.quantidade());
        assertEquals(50, resposta.estoqueAnterior());
        assertEquals(70, resposta.estoquePosterior());
        assertEquals("Reposicao de mercadoria", resposta.motivo());
        assertNotNull(resposta.dataMovimentacao());

        // confirma que não existe um pedido associado
        assertNull(resposta.pedidoId());

        // verifica se o repositório consultou o ID correto
        verify(movimentacaoEstoqueRepository).findById(5L);

        // garante que a consulta não realizou operações de gravação
        verify(movimentacaoEstoqueRepository, never())
                .save(any(MovimentacaoEstoque.class));

        verifyNoInteractions(produtoUnidadeRepository, usuarioRepository);
    }

    @Test
    void deveListarMovimentacoesPorProdutoUnidade() {

        // cria o usuário responsável pelas movimentações
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);

        // cria o produto utilizado no teste
        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        // cria a unidade responsável pelo estoque
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");

        // cria a associação entre produto e unidade
        ProdutoUnidade produtoUnidade = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade, "id", 1L);
        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setEstoque(45);

        // cria uma movimentação de entrada
        MovimentacaoEstoque entrada = new MovimentacaoEstoque();
        ReflectionTestUtils.setField(entrada, "id", 1L);
        entrada.setProdutoUnidade(produtoUnidade);
        entrada.setUsuario(usuario);
        entrada.setTipo(TipoMovimentacaoEstoque.ENTRADA);
        entrada.setQuantidade(60);
        entrada.setEstoqueAnterior(0);
        entrada.setEstoquePosterior(60);
        entrada.setMotivo("Recebimento de mercadoria");
        entrada.prePersist();

        // cria uma movimentação de saída
        MovimentacaoEstoque saida = new MovimentacaoEstoque();
        ReflectionTestUtils.setField(saida, "id", 2L);
        saida.setProdutoUnidade(produtoUnidade);
        saida.setUsuario(usuario);
        saida.setTipo(TipoMovimentacaoEstoque.SAIDA);
        saida.setQuantidade(15);
        saida.setEstoqueAnterior(60);
        saida.setEstoquePosterior(45);
        saida.setMotivo("Saida de mercadoria");
        saida.prePersist();

        // simula a existência da associação entre produto e unidade
        when(produtoUnidadeRepository.existsById(1L))
                .thenReturn(true);

        // simula a consulta das movimentações da associação
        when(movimentacaoEstoqueRepository
                .findByProdutoUnidadeIdOrderByDataMovimentacaoDesc(1L))
                .thenReturn(List.of(saida, entrada));

        // executa a consulta filtrada
        List<MovimentacaoEstoqueResponseDTO> resposta =
                movimentacaoEstoqueService.listarPorProdutoUnidade(1L);

        // verifica a quantidade de movimentações retornadas
        assertEquals(2, resposta.size());

        // confirma que todas pertencem à associação solicitada
        assertTrue(resposta.stream()
                .allMatch(movimentacao ->
                        Long.valueOf(1L).equals(
                                movimentacao.produtoUnidadeId()
                        )
                ));

        // verifica a primeira movimentação retornada
        assertEquals(2L, resposta.get(0).id());
        assertEquals(TipoMovimentacaoEstoque.SAIDA,
                resposta.get(0).tipo());
        assertEquals(45, resposta.get(0).estoquePosterior());

        // verifica a segunda movimentação retornada
        assertEquals(1L, resposta.get(1).id());
        assertEquals(TipoMovimentacaoEstoque.ENTRADA,
                resposta.get(1).tipo());
        assertEquals(60, resposta.get(1).estoquePosterior());

        // confirma que a existência da associação foi verificada
        verify(produtoUnidadeRepository).existsById(1L);

        // confirma que o método correto de consulta foi utilizado
        verify(movimentacaoEstoqueRepository)
                .findByProdutoUnidadeIdOrderByDataMovimentacaoDesc(1L);

        // garante que a consulta não realizou gravações
        verify(produtoUnidadeRepository, never())
                .save(any(ProdutoUnidade.class));

        verify(movimentacaoEstoqueRepository, never())
                .save(any(MovimentacaoEstoque.class));

        // garante que não houve consultas desnecessárias de usuários
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveListarMovimentacoesPorUnidade() {

        // cria o usuário responsável pelas movimentações
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);

        // cria a unidade utilizada no teste
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");

        // cria o primeiro produto
        Produto produto1 = new Produto();
        ReflectionTestUtils.setField(produto1, "id", 1L);
        produto1.setNome("Cuscuz nordestino");

        // cria o segundo produto
        Produto produto2 = new Produto();
        ReflectionTestUtils.setField(produto2, "id", 2L);
        produto2.setNome("Tapioca");

        // associa o primeiro produto à unidade
        ProdutoUnidade produtoUnidade1 = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade1, "id", 1L);
        produtoUnidade1.setProduto(produto1);
        produtoUnidade1.setUnidade(unidade);
        produtoUnidade1.setEstoque(50);

        // associa o segundo produto à mesma unidade
        ProdutoUnidade produtoUnidade2 = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade2, "id", 2L);
        produtoUnidade2.setProduto(produto2);
        produtoUnidade2.setUnidade(unidade);
        produtoUnidade2.setEstoque(30);

        // cria uma movimentação de entrada para o primeiro produto
        MovimentacaoEstoque movimentacao1 = new MovimentacaoEstoque();
        ReflectionTestUtils.setField(movimentacao1, "id", 1L);
        movimentacao1.setProdutoUnidade(produtoUnidade1);
        movimentacao1.setUsuario(usuario);
        movimentacao1.setTipo(TipoMovimentacaoEstoque.ENTRADA);
        movimentacao1.setQuantidade(50);
        movimentacao1.setEstoqueAnterior(0);
        movimentacao1.setEstoquePosterior(50);
        movimentacao1.setMotivo("Reposicao de cuscuz");
        movimentacao1.prePersist();

        // cria uma movimentação de entrada para o segundo produto
        MovimentacaoEstoque movimentacao2 = new MovimentacaoEstoque();
        ReflectionTestUtils.setField(movimentacao2, "id", 2L);
        movimentacao2.setProdutoUnidade(produtoUnidade2);
        movimentacao2.setUsuario(usuario);
        movimentacao2.setTipo(TipoMovimentacaoEstoque.ENTRADA);
        movimentacao2.setQuantidade(30);
        movimentacao2.setEstoqueAnterior(0);
        movimentacao2.setEstoquePosterior(30);
        movimentacao2.setMotivo("Reposicao de tapioca");
        movimentacao2.prePersist();

        // simula a consulta das movimentações da unidade
        when(movimentacaoEstoqueRepository
                .findByProdutoUnidadeUnidadeIdOrderByDataMovimentacaoDesc(1L))
                .thenReturn(List.of(movimentacao2, movimentacao1));

        // executa a consulta filtrada pela unidade
        List<MovimentacaoEstoqueResponseDTO> resposta =
                movimentacaoEstoqueService.listarPorUnidade(1L);

        // confirma a quantidade de movimentações retornadas
        assertEquals(2, resposta.size());

        // verifica se todas pertencem à unidade solicitada
        assertTrue(resposta.stream()
                .allMatch(movimentacao ->
                        Long.valueOf(1L).equals(movimentacao.unidadeId())
                ));

        // verifica os dados da primeira movimentação retornada
        assertEquals(2L, resposta.get(0).id());
        assertEquals(2L, resposta.get(0).produtoId());
        assertEquals("Tapioca", resposta.get(0).produtoNome());
        assertEquals(30, resposta.get(0).estoquePosterior());

        // verifica os dados da segunda movimentação retornada
        assertEquals(1L, resposta.get(1).id());
        assertEquals(1L, resposta.get(1).produtoId());
        assertEquals("Cuscuz nordestino", resposta.get(1).produtoNome());
        assertEquals(50, resposta.get(1).estoquePosterior());

        // confirma que o método correto do repositório foi utilizado
        verify(movimentacaoEstoqueRepository)
                .findByProdutoUnidadeUnidadeIdOrderByDataMovimentacaoDesc(1L);

        // garante que a consulta não realizou gravações
        verify(movimentacaoEstoqueRepository, never())
                .save(any(MovimentacaoEstoque.class));

        // garante que os demais repositórios não foram utilizados
        verifyNoInteractions(produtoUnidadeRepository, usuarioRepository);
    }

    @Test
    void deveListarMovimentacoesPorTipo() {

        // cria o usuário responsável pelas movimentações
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);

        // cria o produto utilizado no teste
        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        // cria a unidade responsável pelo estoque
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");

        // cria a associação entre produto e unidade
        ProdutoUnidade produtoUnidade = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade, "id", 1L);
        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setEstoque(80);

        // cria a primeira movimentação de entrada
        MovimentacaoEstoque entrada1 = new MovimentacaoEstoque();
        ReflectionTestUtils.setField(entrada1, "id", 1L);
        entrada1.setProdutoUnidade(produtoUnidade);
        entrada1.setUsuario(usuario);
        entrada1.setTipo(TipoMovimentacaoEstoque.ENTRADA);
        entrada1.setQuantidade(50);
        entrada1.setEstoqueAnterior(0);
        entrada1.setEstoquePosterior(50);
        entrada1.setMotivo("Recebimento inicial de mercadoria");
        entrada1.prePersist();

        // cria a segunda movimentação de entrada
        MovimentacaoEstoque entrada2 = new MovimentacaoEstoque();
        ReflectionTestUtils.setField(entrada2, "id", 2L);
        entrada2.setProdutoUnidade(produtoUnidade);
        entrada2.setUsuario(usuario);
        entrada2.setTipo(TipoMovimentacaoEstoque.ENTRADA);
        entrada2.setQuantidade(30);
        entrada2.setEstoqueAnterior(50);
        entrada2.setEstoquePosterior(80);
        entrada2.setMotivo("Reposicao de mercadoria");
        entrada2.prePersist();

        // simula a consulta das movimentações do tipo ENTRADA
        when(movimentacaoEstoqueRepository
                .findByTipoOrderByDataMovimentacaoDesc(
                        TipoMovimentacaoEstoque.ENTRADA))
                .thenReturn(List.of(entrada2, entrada1));

        // executa a consulta filtrada pelo tipo de movimentação
        List<MovimentacaoEstoqueResponseDTO> resposta =
                movimentacaoEstoqueService.listarPorTipo(
                        TipoMovimentacaoEstoque.ENTRADA
                );

        // confirma a quantidade de movimentações retornadas
        assertEquals(2, resposta.size());

        // verifica se todas as movimentações são do tipo solicitado
        assertTrue(resposta.stream()
                .allMatch(movimentacao ->
                        movimentacao.tipo() ==
                                TipoMovimentacaoEstoque.ENTRADA
                ));

        // verifica os dados da movimentação mais recente
        assertEquals(2L, resposta.get(0).id());
        assertEquals(30, resposta.get(0).quantidade());
        assertEquals(50, resposta.get(0).estoqueAnterior());
        assertEquals(80, resposta.get(0).estoquePosterior());

        // verifica os dados da movimentação mais antiga
        assertEquals(1L, resposta.get(1).id());
        assertEquals(50, resposta.get(1).quantidade());
        assertEquals(0, resposta.get(1).estoqueAnterior());
        assertEquals(50, resposta.get(1).estoquePosterior());

        // confirma que o repositório recebeu o tipo correto
        verify(movimentacaoEstoqueRepository)
                .findByTipoOrderByDataMovimentacaoDesc(
                        TipoMovimentacaoEstoque.ENTRADA
                );

        // garante que a consulta não realizou gravações
        verify(movimentacaoEstoqueRepository, never())
                .save(any(MovimentacaoEstoque.class));

        // garante que os demais repositórios não foram utilizados
        verifyNoInteractions(produtoUnidadeRepository, usuarioRepository);
    }

    @Test
    void deveRejeitarSaidaComEstoqueInsuficiente() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // cria o produto utilizado no teste
        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        // cria uma unidade ativa
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");
        unidade.setAtiva(true);

        // cria a associação com apenas 35 unidades em estoque
        ProdutoUnidade produtoUnidade = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade, "id", 1L);
        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setEstoque(35);

        // solicita uma saída superior ao estoque disponível
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.SAIDA,
                        40,
                        "Tentativa de retirada acima do saldo"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // simula a localização do estoque pelo método com bloqueio
        when(produtoUnidadeRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(produtoUnidade));

        // executa a operação e verifica se a exceção esperada é lançada
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                )
        );

        // confirma a mensagem da regra de negócio
        assertEquals(
                "Estoque insuficiente para realizar a saída.",
                excecao.getMessage()
        );

        // garante que o estoque original permanece inalterado
        assertEquals(35, produtoUnidade.getEstoque());

        // confirma que o usuário responsável foi consultado
        verify(usuarioRepository)
                .findByEmail("admin@raizes.local");

        // confirma que o estoque foi consultado pelo método com bloqueio
        verify(produtoUnidadeRepository)
                .findByIdComBloqueio(1L);

        // garante que nenhuma alteração de estoque foi salva
        verify(produtoUnidadeRepository, never())
                .save(any(ProdutoUnidade.class));

        // garante que nenhuma movimentação foi registrada
        verifyNoInteractions(movimentacaoEstoqueRepository);
    }

    @Test
    void deveRejeitarMovimentacaoComQuantidadeZero() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // cria uma solicitação com quantidade inválida
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.ENTRADA,
                        0,
                        "Tentativa de entrada com quantidade zero"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // executa a operação e verifica se a exceção esperada é lançada
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                )
        );

        // confirma a mensagem da regra de negócio
        assertEquals(
                "A quantidade deve ser maior que zero.",
                excecao.getMessage()
        );

        // confirma que o usuário responsável foi consultado
        verify(usuarioRepository)
                .findByEmail("admin@raizes.local");

        // garante que nenhuma consulta ou alteração de estoque ocorreu
        verifyNoInteractions(produtoUnidadeRepository);

        // garante que nenhuma movimentação foi registrada
        verifyNoInteractions(movimentacaoEstoqueRepository);
    }

    @Test
    void deveRejeitarMovimentacaoComQuantidadeNegativa() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // cria uma solicitação com quantidade negativa
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.SAIDA,
                        -10,
                        "Tentativa de saida com quantidade negativa"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // executa a operação e verifica se a exceção esperada é lançada
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                )
        );

        // confirma a mensagem da regra de negócio
        assertEquals(
                "A quantidade deve ser maior que zero.",
                excecao.getMessage()
        );

        // confirma que o usuário responsável foi consultado
        verify(usuarioRepository)
                .findByEmail("admin@raizes.local");

        // garante que nenhuma consulta ou alteração de estoque ocorreu
        verifyNoInteractions(produtoUnidadeRepository);

        // garante que nenhuma movimentação foi registrada
        verifyNoInteractions(movimentacaoEstoqueRepository);
    }

    @Test
    void deveRejeitarMovimentacaoComQuantidadeNula() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // cria uma solicitação sem quantidade informada
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.ENTRADA,
                        null,
                        "Tentativa de entrada sem quantidade"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // executa a operação e verifica se a exceção esperada é lançada
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                )
        );

        // confirma a mensagem da regra de negócio
        assertEquals(
                "A quantidade deve ser maior que zero.",
                excecao.getMessage()
        );

        // confirma que o usuário responsável foi consultado
        verify(usuarioRepository)
                .findByEmail("admin@raizes.local");

        // garante que nenhuma consulta ou alteração de estoque ocorreu
        verifyNoInteractions(produtoUnidadeRepository);

        // garante que nenhuma movimentação foi registrada
        verifyNoInteractions(movimentacaoEstoqueRepository);
    }

    @Test
    void deveRejeitarMovimentacaoComUsuarioInexistente() {

        // cria uma solicitação de movimentação com dados válidos
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.ENTRADA,
                        20,
                        "Recebimento de mercadoria"
                );

        // define um e-mail fictício de um usuário inexistente
        String emailInexistente = "inexistente@raizes.local";

        // simula a ausência do usuário no banco de dados
        when(usuarioRepository.findByEmail(emailInexistente))
                .thenReturn(Optional.empty());

        // executa a operação e verifica se a exceção esperada é lançada
        NoSuchElementException excecao = assertThrows(
                NoSuchElementException.class,
                () -> movimentacaoEstoqueService.registrar(
                        dto,
                        emailInexistente
                )
        );

        // confirma a mensagem de erro esperada
        assertEquals(
                "Usuário responsável não encontrado.",
                excecao.getMessage()
        );

        // confirma que o usuário foi consultado pelo e-mail correto
        verify(usuarioRepository)
                .findByEmail(emailInexistente);

        // garante que nenhuma consulta ou alteração de estoque ocorreu
        verifyNoInteractions(produtoUnidadeRepository);

        // garante que nenhuma movimentação foi registrada
        verifyNoInteractions(movimentacaoEstoqueRepository);
    }

    @Test
    void deveRejeitarMovimentacaoComProdutoUnidadeInexistente() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // define o ID fictício de uma associação inexistente
        Long produtoUnidadeId = 99999L;

        // cria uma solicitação de movimentação para essa associação
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        produtoUnidadeId,
                        TipoMovimentacaoEstoque.ENTRADA,
                        20,
                        "Recebimento de mercadoria"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // simula a ausência da associação entre produto e unidade
        when(produtoUnidadeRepository
                .findByIdComBloqueio(produtoUnidadeId))
                .thenReturn(Optional.empty());

        // executa a operação e verifica se a exceção esperada é lançada
        NoSuchElementException excecao = assertThrows(
                NoSuchElementException.class,
                () -> movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                )
        );

        // confirma a mensagem de erro esperada
        assertEquals(
                "Associação de produto e unidade não encontrada: 99999",
                excecao.getMessage()
        );

        // confirma que o usuário responsável foi consultado
        verify(usuarioRepository)
                .findByEmail("admin@raizes.local");

        // confirma que a associação foi consultada pelo ID correto
        verify(produtoUnidadeRepository)
                .findByIdComBloqueio(produtoUnidadeId);

        // garante que nenhuma alteração de estoque foi salva
        verify(produtoUnidadeRepository, never())
                .save(any(ProdutoUnidade.class));

        // garante que nenhuma movimentação foi registrada
        verifyNoInteractions(movimentacaoEstoqueRepository);
    }

    @Test
    void deveRejeitarMovimentacaoEmUnidadeInativa() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // cria o produto utilizado no teste
        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        // cria uma unidade desativada
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");
        unidade.setAtiva(false);

        // cria a associação com um estoque inicial de 50 unidades
        ProdutoUnidade produtoUnidade = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade, "id", 1L);
        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setEstoque(50);

        // cria uma solicitação de entrada de estoque
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.ENTRADA,
                        20,
                        "Reposicao de mercadoria em unidade inativa"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // simula a localização da associação entre produto e unidade
        when(produtoUnidadeRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(produtoUnidade));

        // executa a operação e verifica se a exceção esperada é lançada
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                )
        );

        // confirma a mensagem da regra de negócio
        assertEquals(
                "Não é possível movimentar o estoque de uma unidade inativa.",
                excecao.getMessage()
        );

        // garante que o estoque original permanece inalterado
        assertEquals(50, produtoUnidade.getEstoque());

        // confirma que o usuário responsável foi consultado
        verify(usuarioRepository)
                .findByEmail("admin@raizes.local");

        // confirma que a associação foi consultada corretamente
        verify(produtoUnidadeRepository)
                .findByIdComBloqueio(1L);

        // garante que nenhuma alteração de estoque foi salva
        verify(produtoUnidadeRepository, never())
                .save(any(ProdutoUnidade.class));

        // garante que nenhuma movimentação foi registrada
        verifyNoInteractions(movimentacaoEstoqueRepository);
    }

    @Test
    void deveRejeitarMovimentacaoDoTipoAjuste() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // cria uma solicitação de movimentação do tipo AJUSTE
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.AJUSTE,
                        20,
                        "Tentativa de ajuste de estoque"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // executa a operação e verifica se a exceção esperada é lançada
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                )
        );

        // confirma a mensagem da regra de negócio
        assertEquals(
                "Movimentações do tipo AJUSTE ainda não estão disponíveis.",
                excecao.getMessage()
        );

        // confirma que o usuário responsável foi consultado
        verify(usuarioRepository)
                .findByEmail("admin@raizes.local");

        // garante que nenhuma consulta ou alteração de estoque ocorreu
        verifyNoInteractions(produtoUnidadeRepository);

        // garante que nenhuma movimentação foi registrada
        verifyNoInteractions(movimentacaoEstoqueRepository);
    }

    @Test
    void deveRejeitarMovimentacaoQueExcedeLimiteMaximoDeEstoque() {

        // cria o usuário responsável pela movimentação
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.setEmail("admin@raizes.local");

        // cria o produto utilizado no teste
        Produto produto = new Produto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        produto.setNome("Cuscuz nordestino");

        // cria uma unidade ativa
        Unidade unidade = new Unidade();
        ReflectionTestUtils.setField(unidade, "id", 1L);
        unidade.setNome("Raizes do Nordeste - Centro");
        unidade.setAtiva(true);

        // cria uma associação com o maior estoque suportado por Integer
        ProdutoUnidade produtoUnidade = new ProdutoUnidade();
        ReflectionTestUtils.setField(produtoUnidade, "id", 1L);
        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setEstoque(Integer.MAX_VALUE);

        // solicita uma entrada que ultrapassaria o limite máximo
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        1L,
                        TipoMovimentacaoEstoque.ENTRADA,
                        1,
                        "Tentativa de entrada acima do limite maximo"
                );

        // simula a localização do usuário responsável
        when(usuarioRepository.findByEmail("admin@raizes.local"))
                .thenReturn(Optional.of(usuario));

        // simula a localização do estoque pelo método com bloqueio
        when(produtoUnidadeRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(produtoUnidade));

        // executa a operação e verifica se a exceção esperada é lançada
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> movimentacaoEstoqueService.registrar(
                        dto,
                        "admin@raizes.local"
                )
        );

        // confirma a mensagem da regra de negócio
        assertEquals(
                "A movimentação excede o limite máximo de estoque.",
                excecao.getMessage()
        );

        // garante que o estoque original permanece inalterado
        assertEquals(
                Integer.MAX_VALUE,
                produtoUnidade.getEstoque()
        );

        // confirma que o usuário responsável foi consultado
        verify(usuarioRepository)
                .findByEmail("admin@raizes.local");

        // confirma que o estoque foi consultado corretamente
        verify(produtoUnidadeRepository)
                .findByIdComBloqueio(1L);

        // garante que nenhuma alteração de estoque foi salva
        verify(produtoUnidadeRepository, never())
                .save(any(ProdutoUnidade.class));

        // garante que nenhuma movimentação foi registrada
        verifyNoInteractions(movimentacaoEstoqueRepository);
    }

    @Test
    void deveLancarExcecaoAoBuscarMovimentacaoInexistente() {

        // define o ID fictício de uma movimentação inexistente
        Long movimentacaoId = 99999L;

        // simula a ausência da movimentação no banco de dados
        when(movimentacaoEstoqueRepository.findById(movimentacaoId))
                .thenReturn(Optional.empty());

        // executa a busca e verifica se a exceção esperada é lançada
        NoSuchElementException excecao = assertThrows(
                NoSuchElementException.class,
                () -> movimentacaoEstoqueService.buscarPorId(
                        movimentacaoId
                )
        );

        // confirma a mensagem de erro esperada
        assertEquals(
                "Movimentação de estoque não encontrada: 99999",
                excecao.getMessage()
        );

        // confirma que o repositório consultou o ID correto
        verify(movimentacaoEstoqueRepository)
                .findById(movimentacaoId);

        // garante que nenhuma movimentação foi salva
        verify(movimentacaoEstoqueRepository, never())
                .save(any(MovimentacaoEstoque.class));

        // garante que nenhuma outra operação ocorreu no repositório
        verifyNoMoreInteractions(movimentacaoEstoqueRepository);

        // garante que os demais repositórios não foram utilizados
        verifyNoInteractions(produtoUnidadeRepository, usuarioRepository);
    }

    @Test
    void deveLancarExcecaoAoFiltrarPorProdutoUnidadeInexistente() {

        // define o ID fictício de uma associação inexistente
        Long produtoUnidadeId = 99999L;

        // simula a ausência da associação entre produto e unidade
        when(produtoUnidadeRepository.existsById(produtoUnidadeId))
                .thenReturn(false);

        // executa a consulta e verifica se a exceção esperada é lançada
        NoSuchElementException excecao = assertThrows(
                NoSuchElementException.class,
                () -> movimentacaoEstoqueService.listarPorProdutoUnidade(
                        produtoUnidadeId
                )
        );

        // confirma a mensagem de erro esperada
        assertEquals(
                "Associação de produto e unidade não encontrada: 99999",
                excecao.getMessage()
        );

        // confirma que a existência da associação foi verificada
        verify(produtoUnidadeRepository)
                .existsById(produtoUnidadeId);

        // garante que nenhuma alteração de estoque foi salva
        verify(produtoUnidadeRepository, never())
                .save(any(ProdutoUnidade.class));

        // garante que nenhuma outra operação ocorreu nesse repositório
        verifyNoMoreInteractions(produtoUnidadeRepository);

        // garante que o histórico não foi consultado ou alterado
        verifyNoInteractions(movimentacaoEstoqueRepository);

        // garante que nenhuma consulta de usuário foi realizada
        verifyNoInteractions(usuarioRepository);
    }
}