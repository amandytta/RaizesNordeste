package br.com.raizesnordeste.raizesnordeste.integration;

import br.com.raizesnordeste.raizesnordeste.service.MovimentacaoEstoqueService;
import br.com.raizesnordeste.raizesnordeste.dto.MovimentacaoEstoqueRequestDTO;
import br.com.raizesnordeste.raizesnordeste.enums.TipoMovimentacaoEstoque;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

// carrega o contexto completo da aplicacao Spring Boot
@SpringBootTest

// ativa o perfil exclusivo dos testes de integracao
@ActiveProfiles("test")
class MovimentacaoEstoqueIntegrationTest {

    // injeta o servico real de movimentacao de estoque
    @Autowired
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    // injeta o componente para executar comandos SQL
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // injeta o componente responsável pela criptografia de senhas por hash
    @Autowired
    private PasswordEncoder passwordEncoder;

    // armazena os identificadores dos registros fictícios
    private Long categoriaId;
    private Long unidadeId;
    private Long produtoId;
    private Long produtoUnidadeId;
    private Long usuarioId;

    // armazena o e-mail do usuário responsável pela movimentação
    private String emailUsuarioTeste;

    // define o estoque inicial utilizado nos testes
    private static final int ESTOQUE_INICIAL = 50;

    // executa a verificacao de seguranca antes de cada teste
    @BeforeEach
    void verificarAmbienteDeTestes() {

        // identifica o banco de dados conectado
        String bancoAtual = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        // garante que estamos utilizando o banco de testes
        assertEquals("raizes_nordeste_test", bancoAtual);

        // identifica o usuario autenticado no MySQL
        String usuarioAtual = jdbcTemplate.queryForObject(
                "SELECT CURRENT_USER()",
                String.class
        );

        // garante que estamos utilizando o usuario exclusivo
        assertEquals("raizes_test@localhost", usuarioAtual);
    }

    private void prepararDadosDeTeste() {

        // gera um identificador único para os registros de cada teste
        String identificador = UUID.randomUUID().toString();

        // define um e-mail exclusivo para o usuário fictício
        emailUsuarioTeste = "estoque-" + identificador + "@teste.local";

        // cadastra uma categoria ativa
        jdbcTemplate.update("""
            INSERT INTO categoria (nome, descricao, ativa)
            VALUES (?, ?, ?)
            """,
                "Categoria " + identificador,
                "Categoria exclusiva para testes de integracao",
                true
        );

        // recupera o ID da categoria cadastrada
        categoriaId = jdbcTemplate.queryForObject(
                "SELECT id FROM categoria WHERE nome = ?",
                Long.class,
                "Categoria " + identificador
        );

        // gera um CNPJ fictício de 14 dígitos
        String cnpjTeste = String.format(
                "%014d",
                Math.floorMod(UUID.randomUUID().getLeastSignificantBits(),
                        100000000000000L)
        );

        // cadastra uma unidade ativa
        jdbcTemplate.update("""
            INSERT INTO unidade (
                nome, cnpj, cep, logradouro,
                numero, bairro, cidade, estado, ativa
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                "Unidade " + identificador,
                cnpjTeste,
                "50000000",
                "Rua de Teste",
                "100",
                "Centro",
                "Recife",
                "PE",
                true
        );

        // recupera o ID da unidade cadastrada
        unidadeId = jdbcTemplate.queryForObject(
                "SELECT id FROM unidade WHERE cnpj = ?",
                Long.class,
                cnpjTeste
        );

        // cadastra um produto ativo associado à categoria
        jdbcTemplate.update("""
            INSERT INTO produto (
                categoria_id, nome, descricao, ativo
            )
            VALUES (?, ?, ?, ?)
            """,
                categoriaId,
                "Cuscuz de teste",
                "Produto exclusivo para testes de integracao",
                true
        );

        // recupera o ID do produto cadastrado
        produtoId = jdbcTemplate.queryForObject(
                "SELECT id FROM produto WHERE categoria_id = ?",
                Long.class,
                categoriaId
        );

        // associa o produto à unidade com o estoque inicial definido
        jdbcTemplate.update("""
            INSERT INTO produto_unidade (
                produto_id, unidade_id, preco,
                estoque, disponivel
            )
            VALUES (?, ?, ?, ?, ?)
            """,
                produtoId,
                unidadeId,
                new BigDecimal("12.90"),
                ESTOQUE_INICIAL,
                true
        );

        // recupera o ID da associação entre produto e unidade
        produtoUnidadeId = jdbcTemplate.queryForObject("""
            SELECT id FROM produto_unidade
            WHERE produto_id = ? AND unidade_id = ?
            """,
                Long.class,
                produtoId,
                unidadeId
        );

        // cadastra o usuário fictício com uma senha protegida por hash
        jdbcTemplate.update("""
            INSERT INTO usuario (
                nome, email, senha_hash, ativo
            )
            VALUES (?, ?, ?, ?)
            """,
                "Usuario de teste de estoque",
                emailUsuarioTeste,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                true
        );

        // recupera o ID do usuário cadastrado
        usuarioId = jdbcTemplate.queryForObject(
                "SELECT id FROM usuario WHERE email = ?",
                Long.class,
                emailUsuarioTeste
        );

        // Recupera o perfil administrativo necessário para movimentar estoque.
        Long perfilAdminId = jdbcTemplate.queryForObject(
                "SELECT id FROM perfil WHERE nome = ?",
                Long.class,
                "ADMIN"
        );

        // Vincula o perfil ao usuário fictício, somente no banco de testes.
        jdbcTemplate.update("""
                INSERT INTO usuario_perfil (usuario_id, perfil_id)
                VALUES (?, ?)
                """,
                usuarioId,
                perfilAdminId
        );
    }

    // executa a limpeza após cada teste, mesmo quando ele falha
    @AfterEach
    void limparDadosDeTeste() {

        // confirma o banco e o usuário antes de excluir registros
        verificarAmbienteDeTestes();

        // remove as movimentações vinculadas ao estoque fictício
        if (produtoUnidadeId != null) {
            jdbcTemplate.update(
                    "DELETE FROM movimentacao_estoque WHERE produto_unidade_id = ?",
                    produtoUnidadeId
            );

            // remove a associação entre produto e unidade
            jdbcTemplate.update(
                    "DELETE FROM produto_unidade WHERE id = ?",
                    produtoUnidadeId
            );
        }

        // remove o produto fictício
        if (produtoId != null) {
            jdbcTemplate.update(
                    "DELETE FROM produto WHERE id = ?",
                    produtoId
            );
        }

        // remove a categoria fictícia
        if (categoriaId != null) {
            jdbcTemplate.update(
                    "DELETE FROM categoria WHERE id = ?",
                    categoriaId
            );
        }

        // remove a unidade fictícia
        if (unidadeId != null) {
            jdbcTemplate.update(
                    "DELETE FROM unidade WHERE id = ?",
                    unidadeId
            );
        }

        // remove o perfil associado antes de excluir o usuário fictício
        if (usuarioId != null) {
            // remove a auditoria do usuário antes de excluí-lo
            jdbcTemplate.update("DELETE FROM auditoria WHERE usuario_id = ?", usuarioId);
            // Respeita a chave estrangeira da tabela usuario_perfil.
            jdbcTemplate.update(
                    "DELETE FROM usuario_perfil WHERE usuario_id = ?",
                    usuarioId
            );

            // Remove somente o usuário criado para este teste.
            jdbcTemplate.update(
                    "DELETE FROM usuario WHERE id = ?",
                    usuarioId
            );
        }
    }

    @Test
    void deveImpedirSaidasConcorrentesComEstoqueInsuficiente()
            throws Exception {

        // prepara os registros fictícios no banco de testes
        prepararDadosDeTeste();

        // cria uma solicitação de saída de 40 unidades
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        produtoUnidadeId,
                        TipoMovimentacaoEstoque.SAIDA,
                        40,
                        "Teste de concorrencia"
                );

        // cria um executor com duas threads independentes
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // controla a preparação das duas threads
        CountDownLatch threadsProntas = new CountDownLatch(2);

        // permite liberar as duas solicitações simultaneamente
        CountDownLatch inicio = new CountDownLatch(1);

        try {

            // envia a primeira solicitação de movimentação
            Future<Boolean> primeiraSolicitacao = executor.submit(() -> {

                // informa que a primeira thread está pronta
                threadsProntas.countDown();

                // aguarda a liberação para iniciar a operação
                assertTrue(inicio.await(10, TimeUnit.SECONDS));

                return executarSaidaConcorrente(dto);
            });

            // envia a segunda solicitação de movimentação
            Future<Boolean> segundaSolicitacao = executor.submit(() -> {

                // informa que a segunda thread está pronta
                threadsProntas.countDown();

                // aguarda a liberação para iniciar a operação
                assertTrue(inicio.await(10, TimeUnit.SECONDS));

                return executarSaidaConcorrente(dto);
            });

            // aguarda até que as duas threads estejam preparadas
            assertTrue(threadsProntas.await(10, TimeUnit.SECONDS));

            // libera as duas solicitações para execução concorrente
            inicio.countDown();

            // recupera o resultado das duas operações
            boolean primeiraAprovada =
                    primeiraSolicitacao.get(30, TimeUnit.SECONDS);

            boolean segundaAprovada =
                    segundaSolicitacao.get(30, TimeUnit.SECONDS);

            // calcula a quantidade de solicitações aprovadas
            int totalAprovadas =
                    (primeiraAprovada ? 1 : 0)
                            + (segundaAprovada ? 1 : 0);

            // garante que somente uma saída foi aprovada
            assertEquals(1, totalAprovadas);

            // consulta o estoque final diretamente no MySQL
            Integer estoqueFinal = jdbcTemplate.queryForObject(
                    "SELECT estoque FROM produto_unidade WHERE id = ?",
                    Integer.class,
                    produtoUnidadeId
            );

            // garante que o estoque final é de 10 unidades
            assertEquals(10, estoqueFinal);

            // consulta a quantidade de movimentações registradas
            Long totalMovimentacoes = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM movimentacao_estoque
                    WHERE produto_unidade_id = ?
                    """,
                    Long.class,
                    produtoUnidadeId
            );

            // garante que somente uma movimentação foi registrada
            assertEquals(1L, totalMovimentacoes);

        } finally {

            // libera as threads caso ocorra uma falha antes do início
            inicio.countDown();

            // encerra o executor e interrompe tarefas pendentes
            executor.shutdownNow();

            // aguarda o encerramento das threads antes da limpeza
            assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
        }
    }

    private boolean executarSaidaConcorrente(
            MovimentacaoEstoqueRequestDTO dto) {

        try {

            // executa a movimentação utilizando o serviço real
            movimentacaoEstoqueService.registrar(
                    dto,
                    emailUsuarioTeste
            );

            // indica que a movimentação foi aprovada
            return true;

        } catch (IllegalArgumentException excecao) {

            // confirma que a rejeição ocorreu por estoque insuficiente
            assertEquals(
                    "Estoque insuficiente para realizar a saída.",
                    excecao.getMessage()
            );

            // indica que a movimentação foi rejeitada
            return false;
        }
    }

    @Test
    void deveDesfazerAlteracaoDeEstoqueQuandoFalharRegistroDaMovimentacao() {

        // prepara os registros fictícios no banco de testes
        prepararDadosDeTeste();

        // cria um motivo com 256 caracteres, ultrapassando o limite de 255 caracteres definido na tabela
        String motivoInvalido = "A".repeat(256);

        // cria uma solicitação de entrada de 20 unidades
        MovimentacaoEstoqueRequestDTO dto =
                new MovimentacaoEstoqueRequestDTO(
                        produtoUnidadeId,
                        TipoMovimentacaoEstoque.ENTRADA,
                        20,
                        motivoInvalido
                );

        // executa a movimentação e espera uma falha de integridade durante a persistência dos dados
        assertThrows(
                DataIntegrityViolationException.class,
                () -> movimentacaoEstoqueService.registrar(
                        dto,
                        emailUsuarioTeste
                )
        );

        // consulta o saldo após a falha da transação
        Integer estoqueFinal = jdbcTemplate.queryForObject(
                "SELECT estoque FROM produto_unidade WHERE id = ?",
                Integer.class,
                produtoUnidadeId
        );

        // garante que o estoque permaneceu com o valor original
        assertEquals(ESTOQUE_INICIAL, estoqueFinal);

        // consulta a quantidade de movimentações persistidas
        Long totalMovimentacoes = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM movimentacao_estoque
                WHERE produto_unidade_id = ?
                """,
                Long.class,
                produtoUnidadeId
        );

        // garante que nenhuma movimentação foi persistida
        assertEquals(0L, totalMovimentacoes);
    }
}