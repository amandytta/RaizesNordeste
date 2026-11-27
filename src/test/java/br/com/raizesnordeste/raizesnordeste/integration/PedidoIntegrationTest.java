package br.com.raizesnordeste.raizesnordeste.integration;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// valida o fluxo de pedidos utilizando a API e o banco de testes
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedidoIntegrationTest {

    // senha utilizada exclusivamente pelos usuários fictícios dos testes
    private static final String SENHA_TESTE = "SenhaPedido@2026!";

    // dependências utilizadas durante os testes
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // armazena os dados criados para permitir uma limpeza específica
    private final List<String> emailsTeste = new ArrayList<>();
    private final List<String> cnpjsTeste = new ArrayList<>();
    private final List<String> categoriasTeste = new ArrayList<>();

    // confirma que os testes estão utilizando o ambiente isolado
    @BeforeEach
    void verificarAmbienteDeTestes() {

        assertEquals(
                "raizes_nordeste_test",
                jdbcTemplate.queryForObject(
                        "SELECT DATABASE()",
                        String.class
                )
        );

        assertEquals(
                "raizes_test@localhost",
                jdbcTemplate.queryForObject(
                        "SELECT CURRENT_USER()",
                        String.class
                )
        );
    }

    // teste 01: valida a criação do pedido e os cálculos monetários
    @Test
    void deveCadastrarPedidoComItensEValoresCorretos() throws Exception {

        String email = criarUsuarioTeste("CLIENTE");
        String token = autenticarUsuario(email);

        Long unidadeId = criarUnidadeTeste();

        Long produtoA = criarProdutoUnidadeTeste(
                unidadeId, "15.00", 50
        );

        Long produtoB = criarProdutoUnidadeTeste(
                unidadeId, "12.00", 30
        );

        // solicita dois produtos com quantidades diferentes
        String json = criarJsonPedido(
                unidadeId,
                "APP",
                criarJsonItem(produtoA, 2) + ","
                        + criarJsonItem(produtoB, 1)
        );

        // verifica o cadastro e os valores calculados pelo backend
        String resposta = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.unidadeId").value(unidadeId))
                .andExpect(jsonPath("$.usuarioId").value(buscarUsuarioId(email)))
                .andExpect(jsonPath("$.canalPedido").value("APP"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.valorTotal").value(42.00))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(15.00))
                .andExpect(jsonPath("$.itens[0].subtotal").value(30.00))
                .andExpect(jsonPath("$.itens[1].subtotal").value(12.00))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // recupera o identificador do pedido criado
        Number identificador = JsonPath.read(resposta, "$.id");
        Long pedidoId = identificador.longValue();

        // verifica se os dois itens foram persistidos
        assertEquals(
                2,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM item_pedido WHERE pedido_id = ?",
                        Integer.class,
                        pedidoId
                )
        );

        // verifica o valor total armazenado no banco
        BigDecimal valorTotal = jdbcTemplate.queryForObject(
                "SELECT valor_total FROM pedido WHERE id = ?",
                BigDecimal.class,
                pedidoId
        );

        assertEquals(
                0,
                new BigDecimal("42.00").compareTo(valorTotal)
        );

        // confirma que o estoque ainda não sofreu alterações
        assertEquals(50, consultarEstoque(produtoA));
        assertEquals(30, consultarEstoque(produtoB));

        // confirma que não foram geradas movimentações de estoque
        assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM movimentacao_estoque WHERE pedido_id = ?",
                        Integer.class,
                        pedidoId
                )
        );
    }

    // teste 02: valida os cinco canais e a filtragem dos pedidos
    @Test
    void deveConsultarEFiltrarPedidosPorCanal() throws Exception {

        String email = criarUsuarioTeste("ADMIN");
        String token = autenticarUsuario(email);

        Long unidadeId = criarUnidadeTeste();

        Long produtoId = criarProdutoUnidadeTeste(
                unidadeId, "15.00", 50
        );

        // cadastra um pedido para cada canal previsto
        for (String canal : List.of(
                "APP", "TOTEM", "BALCAO", "PICKUP", "WEB"
        )) {

            cadastrarPedidoTeste(
                    token,
                    unidadeId,
                    canal,
                    produtoId,
                    1
            );
        }

        // verifica os cinco pedidos da unidade
        mockMvc.perform(get("/api/pedidos/unidade/{unidadeId}", unidadeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        // verifica a filtragem por canal dentro da unidade
        mockMvc.perform(get("/api/pedidos/unidade/{unidadeId}", unidadeId)
                        .param("canalPedido", "TOTEM")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].canalPedido").value("TOTEM"));

        // verifica a filtragem no histórico global
        mockMvc.perform(get("/api/pedidos")
                        .param("canalPedido", "WEB")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.unidadeId == " + unidadeId + ")]"
                ).isNotEmpty())
                .andExpect(jsonPath(
                        "$[?(@.canalPedido != 'WEB')]"
                ).isEmpty());
    }

    // teste 03: valida a consulta do histórico pessoal do cliente
    @Test
    void deveConsultarSomenteOsPropriosPedidos() throws Exception {

        String emailA = criarUsuarioTeste("CLIENTE");
        String emailB = criarUsuarioTeste("CLIENTE");

        String tokenA = autenticarUsuario(emailA);
        String tokenB = autenticarUsuario(emailB);

        Long unidadeId = criarUnidadeTeste();

        Long produtoId = criarProdutoUnidadeTeste(
                unidadeId, "15.00", 50
        );

        // cria dois pedidos para o primeiro cliente
        Long pedidoId = cadastrarPedidoTeste(
                tokenA, unidadeId, "APP", produtoId, 1
        );

        cadastrarPedidoTeste(
                tokenA, unidadeId, "WEB", produtoId, 2
        );

        // cria um pedido pertencente a outro cliente
        cadastrarPedidoTeste(
                tokenB, unidadeId, "APP", produtoId, 1
        );

        // confirma que apenas os pedidos do primeiro cliente são retornados
        mockMvc.perform(get("/api/pedidos/meus")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // verifica o filtro por canal no histórico pessoal
        mockMvc.perform(get("/api/pedidos/meus")
                        .param("canalPedido", "APP")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(pedidoId))
                .andExpect(jsonPath("$[0].usuarioId")
                        .value(buscarUsuarioId(emailA)));

        // verifica a consulta individual do próprio pedido
        mockMvc.perform(get("/api/pedidos/{id}", pedidoId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedidoId));
    }

    // teste 04: impede a criação de pedidos sem autenticação
    @Test
    void deveNegarCadastroSemAutenticacao() throws Exception {

        Long unidadeId = criarUnidadeTeste();

        Long produtoId = criarProdutoUnidadeTeste(
                unidadeId, "15.00", 50
        );

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarJsonPedido(
                                unidadeId,
                                "APP",
                                criarJsonItem(produtoId, 1)
                        )))
                .andExpect(status().isUnauthorized());

        // confirma que nenhum pedido foi persistido
        assertEquals(0, contarPedidos(unidadeId));
    }

    // teste 05: impede a criação de pedidos sem itens
    @Test
    void deveRejeitarPedidoSemItens() throws Exception {

        String token = autenticarUsuario(
                criarUsuarioTeste("CLIENTE")
        );

        Long unidadeId = criarUnidadeTeste();

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarJsonPedido(
                                unidadeId, "APP", ""
                        )))
                .andExpect(status().isBadRequest());

        assertEquals(0, contarPedidos(unidadeId));
    }

    // teste 06: impede quantidades iguais a zero ou negativas
    @Test
    void deveRejeitarQuantidadeInvalida() throws Exception {

        String token = autenticarUsuario(
                criarUsuarioTeste("CLIENTE")
        );

        Long unidadeId = criarUnidadeTeste();

        Long produtoId = criarProdutoUnidadeTeste(
                unidadeId, "15.00", 50
        );

        // verifica as duas quantidades inválidas
        for (int quantidade : List.of(0, -1)) {

            mockMvc.perform(post("/api/pedidos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(criarJsonPedido(
                                    unidadeId,
                                    "APP",
                                    criarJsonItem(produtoId, quantidade)
                            )))
                    .andExpect(status().isBadRequest());
        }

        assertEquals(0, contarPedidos(unidadeId));
        assertEquals(50, consultarEstoque(produtoId));
    }

    // teste 07: impede pedidos com estoque insuficiente
    @Test
    void deveRejeitarPedidoComEstoqueInsuficiente() throws Exception {

        String token = autenticarUsuario(
                criarUsuarioTeste("CLIENTE")
        );

        Long unidadeId = criarUnidadeTeste();

        Long produtoId = criarProdutoUnidadeTeste(
                unidadeId, "15.00", 5
        );

        // solicita uma quantidade superior ao saldo disponível
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarJsonPedido(
                                unidadeId,
                                "APP",
                                criarJsonItem(produtoId, 6)
                        )))
                .andExpect(status().isConflict());

        assertEquals(0, contarPedidos(unidadeId));
        assertEquals(5, consultarEstoque(produtoId));
    }

    // teste 08: impede a inclusão de produtos pertencentes a outra unidade
    @Test
    void deveRejeitarProdutoDeOutraUnidade() throws Exception {

        String token = autenticarUsuario(
                criarUsuarioTeste("CLIENTE")
        );

        Long unidadeA = criarUnidadeTeste();
        Long unidadeB = criarUnidadeTeste();

        Long produtoA = criarProdutoUnidadeTeste(
                unidadeA, "15.00", 50
        );

        Long produtoB = criarProdutoUnidadeTeste(
                unidadeB, "12.00", 50
        );

        // inclui um item válido e outro pertencente a uma unidade diferente
        String json = criarJsonPedido(
                unidadeA,
                "APP",
                criarJsonItem(produtoA, 1) + ","
                        + criarJsonItem(produtoB, 1)
        );

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());

        // confirma que nenhum pedido foi persistido parcialmente
        assertEquals(0, contarPedidos(unidadeA));
        assertEquals(0, contarPedidos(unidadeB));
        assertEquals(50, consultarEstoque(produtoA));
        assertEquals(50, consultarEstoque(produtoB));
    }

    // teste 09: impede produtos duplicados no mesmo pedido
    @Test
    void deveRejeitarProdutosDuplicados() throws Exception {

        String token = autenticarUsuario(
                criarUsuarioTeste("CLIENTE")
        );

        Long unidadeId = criarUnidadeTeste();

        Long produtoId = criarProdutoUnidadeTeste(
                unidadeId, "15.00", 50
        );

        // repete a mesma associação de produto e unidade
        String json = criarJsonPedido(
                unidadeId,
                "APP",
                criarJsonItem(produtoId, 1) + ","
                        + criarJsonItem(produtoId, 2)
        );

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());

        assertEquals(0, contarPedidos(unidadeId));
        assertEquals(50, consultarEstoque(produtoId));
    }

    // teste 10: impede o acesso ao pedido de outro cliente
    @Test
    void deveNegarConsultaDePedidoDeOutroCliente() throws Exception {

        String tokenA = autenticarUsuario(
                criarUsuarioTeste("CLIENTE")
        );

        String tokenB = autenticarUsuario(
                criarUsuarioTeste("CLIENTE")
        );

        Long unidadeId = criarUnidadeTeste();

        Long produtoId = criarProdutoUnidadeTeste(
                unidadeId, "15.00", 50
        );

        Long pedidoId = cadastrarPedidoTeste(
                tokenA, unidadeId, "APP", produtoId, 1
        );

        // retorna 404 para não revelar a existência do pedido
        mockMvc.perform(get("/api/pedidos/{id}", pedidoId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    // teste 11: restringe as consultas administrativas
    @Test
    void deveNegarHistoricoAdministrativoAoCliente() throws Exception {

        String token = autenticarUsuario(
                criarUsuarioTeste("CLIENTE")
        );

        Long unidadeId = criarUnidadeTeste();

        // impede o acesso ao histórico global
        mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // impede o acesso ao histórico completo de uma unidade
        mockMvc.perform(get("/api/pedidos/unidade/{unidadeId}", unidadeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // teste 12: valida as permissões dos funcionários por unidade
    @Test
    void deveRespeitarPermissoesDosFuncionariosPorUnidade() throws Exception {

        String emailAtendente = criarUsuarioTeste("ATENDENTE");
        String emailGerente = criarUsuarioTeste("GERENTE");

        String tokenAdmin = autenticarUsuario(
                criarUsuarioTeste("ADMIN")
        );

        Long unidadeA = criarUnidadeTeste();
        Long unidadeB = criarUnidadeTeste();

        Long produtoA = criarProdutoUnidadeTeste(
                unidadeA, "15.00", 50
        );

        Long produtoB = criarProdutoUnidadeTeste(
                unidadeB, "15.00", 50
        );

        // cria um pedido em uma unidade não autorizada aos funcionários
        Long pedidoB = cadastrarPedidoTeste(
                tokenAdmin, unidadeB, "BALCAO", produtoB, 1
        );

        // verifica as permissões de atendentes e gerentes
        for (String email : List.of(emailAtendente, emailGerente)) {

            // vincula o funcionário exclusivamente à unidade A
            vincularFuncionarioTeste(email, unidadeA);

            String token = autenticarUsuario(email);

            // permite cadastrar pedidos na unidade autorizada
            Long pedidoA = cadastrarPedidoTeste(
                    token, unidadeA, "BALCAO", produtoA, 1
            );

            // permite consultar o pedido da unidade autorizada
            mockMvc.perform(get("/api/pedidos/{id}", pedidoA)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // permite consultar o histórico da unidade autorizada
            mockMvc.perform(get("/api/pedidos/unidade/{unidadeId}", unidadeA)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // impede cadastrar pedidos em outra unidade
            mockMvc.perform(post("/api/pedidos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(criarJsonPedido(
                                    unidadeB,
                                    "BALCAO",
                                    criarJsonItem(produtoB, 1)
                            )))
                    .andExpect(status().isForbidden());

            // impede consultar pedidos de outra unidade
            mockMvc.perform(get("/api/pedidos/{id}", pedidoB)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/pedidos/unidade/{unidadeId}", unidadeB)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());

            // impede o acesso ao histórico global
            mockMvc.perform(get("/api/pedidos")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());

            // impede o uso do histórico pessoal por funcionários
            mockMvc.perform(get("/api/pedidos/meus")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());

            // desativa o vínculo do funcionário com a unidade
            jdbcTemplate.update(
                    """
                    UPDATE usuario_unidade
                    SET ativo = FALSE
                    WHERE usuario_id = ? AND unidade_id = ?
                    """,
                    buscarUsuarioId(email),
                    unidadeA
            );

            // confirma que o mesmo token não permite contornar a revogação
            mockMvc.perform(get("/api/pedidos/{id}", pedidoA)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        // confirma que somente os pedidos autorizados foram persistidos
        assertEquals(2, contarPedidos(unidadeA));
        assertEquals(1, contarPedidos(unidadeB));
    }

    // reúne os dados de um pedido fictício para os cenários de pagamento
    private record CenarioPagamento(String token, Long unidade, Long produto, Long pedido) {}

    // prepara um pedido ainda não pago, com duas unidades solicitadas
    private CenarioPagamento prepararPagamento() throws Exception {
        String token = autenticarUsuario(criarUsuarioTeste("CLIENTE"));
        Long unidade = criarUnidadeTeste();
        Long produto = criarProdutoUnidadeTeste(unidade, "15.00", 50);
        Long pedido = cadastrarPedidoTeste(token, unidade, "APP", produto, 2);
        return new CenarioPagamento(token, unidade, produto, pedido);
    }

    // envia uma tentativa de pagamento pela API autenticada
    private ResultActions pagar(String token, Long pedido, String resultado) throws Exception {
        return mockMvc.perform(post("/api/pedidos/{id}/pagamentos", pedido)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"metodo":"PIX","resultadoSimulado":"%s"}
                        """.formatted(resultado)));
    }

    // envia uma transição operacional pela API
    private ResultActions alterarStatus(String token, Long pedido, String novoStatus) throws Exception {
        return mockMvc.perform(patch("/api/pedidos/{id}/status", pedido)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s"}
                        """.formatted(novoStatus)));
    }

    // solicita o cancelamento pela API
    private ResultActions cancelarPedido(String token, Long pedido) throws Exception {
        return mockMvc.perform(patch("/api/pedidos/{id}/cancelamento", pedido)
                .header("Authorization", "Bearer " + token));
    }

    // verifica diretamente os efeitos persistidos da operação
    private void conferirPagamento(CenarioPagamento c, String estado, int saldo,
                                   int pagamentos, int movimentos) {
        assertEquals(estado, jdbcTemplate.queryForObject(
                "SELECT status FROM pedido WHERE id = ?", String.class, c.pedido()));
        assertEquals(saldo, consultarEstoque(c.produto()));
        assertEquals(pagamentos, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pagamento WHERE pedido_id = ?", Integer.class, c.pedido()));
        assertEquals(movimentos, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM movimentacao_estoque WHERE pedido_id = ?", Integer.class, c.pedido()));
    }

    // teste 13: confirma aprovação, valor e movimentação automática
    @Test
    void deveAprovarPagamentoEBaixarEstoque() throws Exception {
        var c = prepararPagamento();
        pagar(c.token(), c.pedido(), "APROVADO")
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.statusPedido").value("RECEBIDO"))
                .andExpect(jsonPath("$.valor").value(30.00));
        conferirPagamento(c, "RECEBIDO", 48, 1, 1);
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM movimentacao_estoque
                WHERE pedido_id = ? AND produto_unidade_id = ? AND tipo = 'SAIDA'
                AND quantidade = 2 AND estoque_anterior = 50 AND estoque_posterior = 48
                AND usuario_id = (SELECT usuario_id FROM pedido WHERE id = ?)
                """, Integer.class, c.pedido(), c.produto(), c.pedido()));
    }

    // teste 14: registra recusa sem alterar estoque ou pedido
    @Test
    void deveRegistrarPagamentoRecusado() throws Exception {
        var c = prepararPagamento();
        pagar(c.token(), c.pedido(), "RECUSADO").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECUSADO"));
        conferirPagamento(c, "AGUARDANDO_PAGAMENTO", 50, 1, 0);
    }

    // teste 15: permite nova tentativa após recusa
    @Test
    void devePermitirNovaTentativa() throws Exception {
        var c = prepararPagamento();
        pagar(c.token(), c.pedido(), "RECUSADO").andExpect(status().isCreated());
        pagar(c.token(), c.pedido(), "APROVADO").andExpect(status().isCreated());
        conferirPagamento(c, "RECEBIDO", 48, 2, 1);
    }

    // teste 16: impede segunda aprovação
    @Test
    void deveImpedirPagamentoDuplicado() throws Exception {
        var c = prepararPagamento();
        pagar(c.token(), c.pedido(), "APROVADO").andExpect(status().isCreated());
        pagar(c.token(), c.pedido(), "APROVADO").andExpect(status().isConflict());
        conferirPagamento(c, "RECEBIDO", 48, 1, 1);
    }

    // teste 17: revalida estoque após a criação do pedido
    @Test
    void deveRevalidarEstoqueNoPagamento() throws Exception {
        var c = prepararPagamento();
        jdbcTemplate.update("UPDATE produto_unidade SET estoque = 1 WHERE id = ?", c.produto());
        pagar(c.token(), c.pedido(), "APROVADO").andExpect(status().isConflict());
        conferirPagamento(c, "AGUARDANDO_PAGAMENTO", 1, 0, 0);
    }

    // teste 18: desfaz a baixa do primeiro item se o segundo falhar
    @Test
    void deveDesfazerBaixaParcial() throws Exception {
        String token = autenticarUsuario(criarUsuarioTeste("CLIENTE"));
        Long unidade = criarUnidadeTeste();
        Long a = criarProdutoUnidadeTeste(unidade, "15.00", 50);
        Long b = criarProdutoUnidadeTeste(unidade, "12.00", 30);
        assertTrue(a < b);
        String resposta = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarJsonPedido(unidade, "APP", criarJsonItem(a, 2) + "," + criarJsonItem(b, 3))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(resposta, "$.id");
        Long pedido = id.longValue();
        jdbcTemplate.update("UPDATE produto_unidade SET estoque = 1 WHERE id = ?", b);
        pagar(token, pedido, "APROVADO").andExpect(status().isConflict());
        conferirPagamento(new CenarioPagamento(token, unidade, a, pedido), "AGUARDANDO_PAGAMENTO", 50, 0, 0);
        assertEquals(1, consultarEstoque(b));
    }

    // teste 19: consulta histórico e recurso individual pelo endereço retornado
    @Test
    void deveConsultarHistoricoDePagamentos() throws Exception {
        var c = prepararPagamento();
        pagar(c.token(), c.pedido(), "RECUSADO").andExpect(status().isCreated());
        String local = pagar(c.token(), c.pedido(), "APROVADO").andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        assertNotNull(local);
        mockMvc.perform(get("/api/pedidos/{id}/pagamentos", c.pedido())
                        .header("Authorization", "Bearer " + c.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("APROVADO"))
                .andExpect(jsonPath("$[1].status").value("RECUSADO"));
        mockMvc.perform(get(local).header("Authorization", "Bearer " + c.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADO"));
    }

    // teste 20: protege pedidos de outros clientes
    @Test
    void deveNegarPagamentoDeOutroCliente() throws Exception {
        var c = prepararPagamento();
        String outro = autenticarUsuario(criarUsuarioTeste("CLIENTE"));
        pagar(outro, c.pedido(), "APROVADO").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/pedidos/{id}/pagamentos", c.pedido())
                .header("Authorization", "Bearer " + outro)).andExpect(status().isNotFound());
        cancelarPedido(outro, c.pedido()).andExpect(status().isNotFound());
        conferirPagamento(c, "AGUARDANDO_PAGAMENTO", 50, 0, 0);
    }

    // teste 21: exige vínculo ativo do funcionário com a unidade
    @Test
    void deveNegarPagamentoSemVinculo() throws Exception {
        var c = prepararPagamento();
        String token = autenticarUsuario(criarUsuarioTeste("ATENDENTE"));
        pagar(token, c.pedido(), "APROVADO").andExpect(status().isForbidden());
        conferirPagamento(c, "AGUARDANDO_PAGAMENTO", 50, 0, 0);
    }

    // teste 22: percorre o fluxo operacional com funcionário autorizado
    @Test
    void deveConcluirFluxoOperacional() throws Exception {
        var c = prepararPagamento();
        String email = criarUsuarioTeste("ATENDENTE");
        vincularFuncionarioTeste(email, c.unidade());
        String token = autenticarUsuario(email);
        pagar(token, c.pedido(), "APROVADO").andExpect(status().isCreated());
        for (String estado : List.of("EM_PREPARO", "PRONTO", "CONCLUIDO")) {
            alterarStatus(token, c.pedido(), estado).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(estado));
        }
        conferirPagamento(c, "CONCLUIDO", 48, 1, 1);
    }

    // teste 23: impede pular etapas ou ignorar o pagamento
    @Test
    void deveRejeitarTransicaoInvalida() throws Exception {
        var c = prepararPagamento();
        String admin = autenticarUsuario(criarUsuarioTeste("ADMIN"));
        alterarStatus(admin, c.pedido(), "RECEBIDO").andExpect(status().isConflict());
        pagar(c.token(), c.pedido(), "APROVADO").andExpect(status().isCreated());
        alterarStatus(admin, c.pedido(), "CONCLUIDO").andExpect(status().isConflict());
        conferirPagamento(c, "RECEBIDO", 48, 1, 1);
    }

    // teste 24: impede cliente de alterar o fluxo operacional
    @Test
    void deveNegarAlteracaoOperacionalAoCliente() throws Exception {
        var c = prepararPagamento();
        pagar(c.token(), c.pedido(), "APROVADO").andExpect(status().isCreated());
        alterarStatus(c.token(), c.pedido(), "EM_PREPARO").andExpect(status().isForbidden());
        conferirPagamento(c, "RECEBIDO", 48, 1, 1);
    }

    // teste 25: cancela pedido não pago preservando histórico de recusa
    @Test
    void deveCancelarPedidoNaoPago() throws Exception {
        var c = prepararPagamento();
        pagar(c.token(), c.pedido(), "RECUSADO").andExpect(status().isCreated());
        cancelarPedido(c.token(), c.pedido()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
        conferirPagamento(c, "CANCELADO", 50, 1, 0);
    }

    // teste 26: impede pagamento após cancelamento
    @Test
    void deveImpedirPagamentoDePedidoCancelado() throws Exception {
        var c = prepararPagamento();
        cancelarPedido(c.token(), c.pedido()).andExpect(status().isOk());
        pagar(c.token(), c.pedido(), "APROVADO").andExpect(status().isConflict());
        conferirPagamento(c, "CANCELADO", 50, 0, 0);
    }

    // teste 27: impede cancelamento de pedido pago sem estorno
    @Test
    void deveImpedirCancelamentoDePedidoPago() throws Exception {
        var c = prepararPagamento();
        pagar(c.token(), c.pedido(), "APROVADO").andExpect(status().isCreated());
        cancelarPedido(c.token(), c.pedido()).andExpect(status().isConflict());
        conferirPagamento(c, "RECEBIDO", 48, 1, 1);
    }

    // teste 28: dispara duas tentativas concorrentes para o mesmo pedido
    @Test
    void deveImpedirAprovacoesConcorrentes() throws Exception {
        var c = prepararPagamento();
        var executor = Executors.newFixedThreadPool(2);
        var prontas = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);
        try {
            // sincroniza o início das duas requisições independentes
            java.util.concurrent.Callable<Integer> tentativa = () -> {
                prontas.countDown();
                if (!inicio.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Tempo de espera excedido.");
                }
                return pagar(c.token(), c.pedido(), "APROVADO")
                        .andReturn().getResponse().getStatus();
            };
            var primeira = executor.submit(tentativa);
            var segunda = executor.submit(tentativa);
            assertTrue(prontas.await(10, TimeUnit.SECONDS));
            inicio.countDown();
            int a = primeira.get(60, TimeUnit.SECONDS);
            int b = segunda.get(60, TimeUnit.SECONDS);
            assertEquals(List.of(201, 409), java.util.stream.Stream.of(a, b).sorted().toList());
            conferirPagamento(c, "RECEBIDO", 48, 1, 1);
        } finally {
            // libera as tarefas e encerra o executor antes da limpeza
            inicio.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
        }
    }

    // cria um usuário fictício com o perfil solicitado
    private String criarUsuarioTeste(String perfil) {

        verificarAmbienteDeTestes();

        // gera um e-mail exclusivo para evitar conflitos
        String email = "pedido.teste."
                + UUID.randomUUID()
                + "@raizes.local";

        emailsTeste.add(email);

        // armazena a senha utilizando BCrypt
        String senhaHash = passwordEncoder.encode(SENHA_TESTE);

        jdbcTemplate.update(
                """
                INSERT INTO usuario (nome, email, senha_hash, ativo)
                VALUES (?, ?, ?, TRUE)
                """,
                "Usuário Pedido Teste",
                email,
                senhaHash
        );

        // recupera o identificador do perfil solicitado
        Long perfilId = jdbcTemplate.queryForObject(
                "SELECT id FROM perfil WHERE nome = ?",
                Long.class,
                perfil
        );

        // associa o usuário ao perfil
        jdbcTemplate.update(
                """
                INSERT INTO usuario_perfil (usuario_id, perfil_id)
                VALUES (?, ?)
                """,
                buscarUsuarioId(email),
                perfilId
        );

        return email;
    }

    // recupera o identificador de um usuário fictício
    private Long buscarUsuarioId(String email) {

        return jdbcTemplate.queryForObject(
                "SELECT id FROM usuario WHERE email = ?",
                Long.class,
                email
        );
    }

    // autentica um usuário pela API e recupera o JWT real
    private String autenticarUsuario(String email) throws Exception {

        String json = """
                {
                    "email": "%s",
                    "senha": "%s"
                }
                """.formatted(email, SENHA_TESTE);

        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(resposta, "$.token");
    }

    // cria uma unidade fictícia com CNPJ exclusivo
    private Long criarUnidadeTeste() {

        verificarAmbienteDeTestes();

        String cnpj;

        // evita conflitos com unidades já existentes
        do {

            cnpj = String.valueOf(
                    ThreadLocalRandom.current().nextLong(
                            10_000_000_000_000L,
                            100_000_000_000_000L
                    )
            );

        } while (jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM unidade WHERE cnpj = ?",
                Integer.class,
                cnpj
        ) > 0);

        cnpjsTeste.add(cnpj);

        // cadastra a unidade utilizada pelo cenário de teste
        jdbcTemplate.update(
                """
                INSERT INTO unidade (
                    nome, cnpj, cep, logradouro, numero,
                    bairro, cidade, estado, ativa
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)
                """,
                "Unidade Pedido Teste",
                cnpj,
                "50000000",
                "Rua de Teste",
                "100",
                "Centro",
                "Recife",
                "PE"
        );

        return jdbcTemplate.queryForObject(
                "SELECT id FROM unidade WHERE cnpj = ?",
                Long.class,
                cnpj
        );
    }

    // cria uma categoria, um produto e sua associação com a unidade
    private Long criarProdutoUnidadeTeste(
            Long unidadeId,
            String preco,
            int estoque
    ) {

        verificarAmbienteDeTestes();

        // gera um nome exclusivo para identificar os dados de teste
        String nomeCategoria = "Pedido " + UUID.randomUUID();

        categoriasTeste.add(nomeCategoria);

        jdbcTemplate.update(
                """
                INSERT INTO categoria (nome, ativa)
                VALUES (?, TRUE)
                """,
                nomeCategoria
        );

        Long categoriaId = jdbcTemplate.queryForObject(
                "SELECT id FROM categoria WHERE nome = ?",
                Long.class,
                nomeCategoria
        );

        // cadastra um produto ativo
        jdbcTemplate.update(
                """
                INSERT INTO produto (categoria_id, nome, ativo)
                VALUES (?, ?, TRUE)
                """,
                categoriaId,
                "Produto Pedido Teste"
        );

        Long produtoId = jdbcTemplate.queryForObject(
                "SELECT id FROM produto WHERE categoria_id = ?",
                Long.class,
                categoriaId
        );

        // define o preço, estoque e disponibilidade comercial
        jdbcTemplate.update(
                """
                INSERT INTO produto_unidade (
                    produto_id, unidade_id, preco, estoque, disponivel
                )
                VALUES (?, ?, ?, ?, TRUE)
                """,
                produtoId,
                unidadeId,
                new BigDecimal(preco),
                estoque
        );

        return jdbcTemplate.queryForObject(
                """
                SELECT id FROM produto_unidade
                WHERE produto_id = ? AND unidade_id = ?
                """,
                Long.class,
                produtoId,
                unidadeId
        );
    }

    // cria um vínculo ativo entre um funcionário e uma unidade
    private void vincularFuncionarioTeste(
            String email,
            Long unidadeId
    ) {

        verificarAmbienteDeTestes();

        jdbcTemplate.update(
                """
                INSERT INTO usuario_unidade (
                    usuario_id, unidade_id, ativo
                )
                VALUES (?, ?, TRUE)
                """,
                buscarUsuarioId(email),
                unidadeId
        );
    }

    // monta o JSON de um item de pedido
    private String criarJsonItem(
            Long produtoUnidadeId,
            int quantidade
    ) {

        return """
                {
                    "produtoUnidadeId": %d,
                    "quantidade": %d
                }
                """.formatted(produtoUnidadeId, quantidade);
    }

    // monta o JSON completo de um pedido
    private String criarJsonPedido(
            Long unidadeId,
            String canalPedido,
            String itens
    ) {

        return """
                {
                    "unidadeId": %d,
                    "canalPedido": "%s",
                    "itens": [%s]
                }
                """.formatted(unidadeId, canalPedido, itens);
    }

    // cadastra um pedido pela API e retorna seu identificador
    private Long cadastrarPedidoTeste(
            String token,
            Long unidadeId,
            String canalPedido,
            Long produtoUnidadeId,
            int quantidade
    ) throws Exception {

        String json = criarJsonPedido(
                unidadeId,
                canalPedido,
                criarJsonItem(produtoUnidadeId, quantidade)
        );

        String resposta = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.canalPedido").value(canalPedido))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number identificador = JsonPath.read(resposta, "$.id");

        return identificador.longValue();
    }

    // consulta o saldo atual de um produto na unidade
    private Integer consultarEstoque(Long produtoUnidadeId) {

        return jdbcTemplate.queryForObject(
                "SELECT estoque FROM produto_unidade WHERE id = ?",
                Integer.class,
                produtoUnidadeId
        );
    }

    // contabiliza os pedidos existentes em uma unidade de teste
    private Integer contarPedidos(Long unidadeId) {

        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedido WHERE unidade_id = ?",
                Integer.class,
                unidadeId
        );
    }

    // remove exclusivamente os registros criados durante cada teste
    @AfterEach
    void limparDadosDeTeste() {

        verificarAmbienteDeTestes();

        // remove os registros dependentes dos pedidos
        for (String cnpj : cnpjsTeste) {

            // remove movimentações eventualmente associadas aos pedidos
            jdbcTemplate.update(
                    """
                    DELETE me FROM movimentacao_estoque me
                    INNER JOIN pedido p ON p.id = me.pedido_id
                    INNER JOIN unidade u ON u.id = p.unidade_id
                    WHERE u.cnpj = ?
                    """,
                    cnpj
            );

            // remove as tentativas de pagamento antes dos pedidos
            jdbcTemplate.update("""
                    DELETE pg FROM pagamento pg
                    INNER JOIN pedido p ON p.id = pg.pedido_id
                    INNER JOIN unidade u ON u.id = p.unidade_id
                    WHERE u.cnpj = ?
                    """, cnpj);

            // remove os itens antes de excluir seus pedidos
            jdbcTemplate.update(
                    """
                    DELETE ip FROM item_pedido ip
                    INNER JOIN pedido p ON p.id = ip.pedido_id
                    INNER JOIN unidade u ON u.id = p.unidade_id
                    WHERE u.cnpj = ?
                    """,
                    cnpj
            );

            // remove os pedidos da unidade fictícia
            jdbcTemplate.update(
                    """
                    DELETE p FROM pedido p
                    INNER JOIN unidade u ON u.id = p.unidade_id
                    WHERE u.cnpj = ?
                    """,
                    cnpj
            );
        }

        // remove os produtos e categorias criados durante os testes
        for (String categoria : categoriasTeste) {

            // remove movimentações associadas aos produtos fictícios
            jdbcTemplate.update(
                    """
                    DELETE me FROM movimentacao_estoque me
                    INNER JOIN produto_unidade pu
                        ON pu.id = me.produto_unidade_id
                    INNER JOIN produto p ON p.id = pu.produto_id
                    INNER JOIN categoria c ON c.id = p.categoria_id
                    WHERE c.nome = ?
                    """,
                    categoria
            );

            // remove as associações entre produtos e unidades
            jdbcTemplate.update(
                    """
                    DELETE pu FROM produto_unidade pu
                    INNER JOIN produto p ON p.id = pu.produto_id
                    INNER JOIN categoria c ON c.id = p.categoria_id
                    WHERE c.nome = ?
                    """,
                    categoria
            );

            // remove os produtos fictícios
            jdbcTemplate.update(
                    """
                    DELETE p FROM produto p
                    INNER JOIN categoria c ON c.id = p.categoria_id
                    WHERE c.nome = ?
                    """,
                    categoria
            );

            // remove a categoria fictícia
            jdbcTemplate.update(
                    "DELETE FROM categoria WHERE nome = ?",
                    categoria
            );
        }

        // remove os usuários e seus relacionamentos
        for (String email : emailsTeste) {

            // remove os vínculos com as unidades
            jdbcTemplate.update(
                    """
                    DELETE uu FROM usuario_unidade uu
                    INNER JOIN usuario u ON u.id = uu.usuario_id
                    WHERE u.email = ?
                    """,
                    email
            );

            // remove as associações com os perfis
            jdbcTemplate.update(
                    """
                    DELETE up FROM usuario_perfil up
                    INNER JOIN usuario u ON u.id = up.usuario_id
                    WHERE u.email = ?
                    """,
                    email
            );

            // remove o usuário fictício
            jdbcTemplate.update(
                    "DELETE FROM usuario WHERE email = ?",
                    email
            );
        }

        // remove as unidades após excluir seus relacionamentos
        for (String cnpj : cnpjsTeste) {

            jdbcTemplate.update(
                    "DELETE FROM unidade WHERE cnpj = ?",
                    cnpj
            );
        }

        // limpa as listas auxiliares utilizadas pelo teste
        emailsTeste.clear();
        cnpjsTeste.clear();
        categoriasTeste.clear();
    }
}