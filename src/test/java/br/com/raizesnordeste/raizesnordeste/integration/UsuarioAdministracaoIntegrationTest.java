package br.com.raizesnordeste.raizesnordeste.integration;

import br.com.raizesnordeste.raizesnordeste.service.PermissaoUnidadeService;
import br.com.raizesnordeste.raizesnordeste.service.UsuarioUnidadeService;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioUnidadeRequestDTO;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


// valida a integração entre a API, a segurança e o banco de dados
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioAdministracaoIntegrationTest {

    // define uma senha exclusiva para os usuários temporários dos testes
    private static final String SENHA_TESTE = "SenhaAdministracao@2026!";

    // permite executar requisições HTTP simuladas
    @Autowired
    private MockMvc mockMvc;

    // permite consultar e preparar registros no banco de testes
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // permite codificar e verificar as senhas dos usuários
    @Autowired
    private PasswordEncoder passwordEncoder;

    // serviço utilizado nos testes de concorrência
    @Autowired
    private UsuarioUnidadeService usuarioUnidadeService;

    // serviço utilizado nos testes de autorização por unidade
    @Autowired
    private PermissaoUnidadeService permissaoUnidadeService;

    // armazena os e-mails dos usuários criados durante cada teste
    private final List<String> emailsUsuariosTeste = new ArrayList<>();

    // armazena os CNPJs das unidades criadas durante cada teste
    private final List<String> cnpjsUnidadesTeste = new ArrayList<>();

    // Registra categorias exclusivas dos testes de estoque para limpeza posterior.
    private final List<String> categoriasEstoqueTeste = new ArrayList<>();

    // verifica se os testes utilizam exclusivamente o ambiente isolado
    @BeforeEach
    void verificarAmbienteDeTestes() {

        String banco = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        String usuarioBanco = jdbcTemplate.queryForObject(
                "SELECT CURRENT_USER()",
                String.class
        );

        // impede operações auxiliares fora do banco de testes
        assertEquals("raizes_nordeste_test", banco);
        assertEquals("raizes_test@localhost", usuarioBanco);
    }

    // gera um e-mail exclusivo e registra-o para limpeza posterior
    private String gerarEmailTeste() {

        String email = "administracao.teste."
                + UUID.randomUUID()
                + "@raizes.local";

        emailsUsuariosTeste.add(email);

        return email;
    }

    // cria um usuário temporário diretamente no banco de testes
    private String criarUsuarioTeste(String perfil, boolean ativo) {

        // confirma novamente o ambiente antes da inserção
        verificarAmbienteDeTestes();

        String email = gerarEmailTeste();

        // codifica a senha utilizando o mesmo encoder da aplicação
        String senhaHash = passwordEncoder.encode(SENHA_TESTE);

        // insere o usuário temporário
        jdbcTemplate.update("""
                INSERT INTO usuario
                    (nome, email, senha_hash, ativo)
                VALUES (?, ?, ?, ?)
                """,
                "Usuário Administração Teste",
                email,
                senhaHash,
                ativo
        );

        // recupera o identificador do usuário criado
        Long usuarioId = buscarUsuarioId(email);

        // recupera o identificador do perfil existente
        Long perfilId = jdbcTemplate.queryForObject(
                "SELECT id FROM perfil WHERE nome = ?",
                Long.class,
                perfil
        );

        // associa o usuário ao perfil informado
        jdbcTemplate.update("""
                INSERT INTO usuario_perfil
                    (usuario_id, perfil_id)
                VALUES (?, ?)
                """,
                usuarioId,
                perfilId
        );

        return email;
    }

    // recupera o identificador de um usuário pelo e-mail
    private Long buscarUsuarioId(String email) {

        return jdbcTemplate.queryForObject(
                "SELECT id FROM usuario WHERE email = ?",
                Long.class,
                email
        );
    }

    // cria uma unidade temporária no banco de testes
    private Long criarUnidadeTeste(boolean ativa) {

        // confirma o ambiente antes da inserção
        verificarAmbienteDeTestes();

        String cnpj;

        // gera um identificador numérico de 14 dígitos ainda não utilizado
        do {
            cnpj = String.valueOf(
                    ThreadLocalRandom.current().nextLong(
                            10_000_000_000_000L,
                            100_000_000_000_000L
                    )
            );
        } while (jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM unidade WHERE cnpj = ?",
                Long.class,
                cnpj
        ) > 0);

        // registra o CNPJ para a limpeza posterior
        cnpjsUnidadesTeste.add(cnpj);

        // insere uma unidade com todos os campos obrigatórios
        jdbcTemplate.update("""
                INSERT INTO unidade
                    (nome, cnpj, cep, logradouro, numero,
                     bairro, cidade, estado, ativa)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "Unidade Administração Teste",
                cnpj,
                "50000000",
                "Rua de Teste",
                "100",
                "Centro",
                "Recife",
                "PE",
                ativa
        );

        // retorna o identificador da unidade criada
        return jdbcTemplate.queryForObject(
                "SELECT id FROM unidade WHERE cnpj = ?",
                Long.class,
                cnpj
        );
    }

    // autentica um usuário temporário e retorna seu token JWT
    private String autenticarUsuario(String email) throws Exception {

        // monta o corpo da requisição de autenticação
        String json = """
                {
                    "email": "%s",
                    "senha": "%s"
                }
                """.formatted(email, SENHA_TESTE);

        // realiza a autenticação pelo endpoint real da aplicação
        String resposta = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // extrai o token sem exibi-lo no terminal
        return JsonPath.read(resposta, "$.token");
    }

    // cria um administrador temporário e realiza sua autenticação
    private String autenticarAdministrador() throws Exception {

        String email = criarUsuarioTeste("ADMIN", true);

        return autenticarUsuario(email);
    }

    // monta o JSON utilizado no cadastro administrativo
    private String criarJsonFuncionario(
            String nome,
            String email,
            String perfil) {

        return """
                {
                    "nome": "%s",
                    "email": "%s",
                    "senha": "%s",
                    "perfil": "%s"
                }
                """.formatted(nome, email, SENHA_TESTE, perfil);
    }

    // monta o JSON utilizado na vinculação de funcionários
    private String criarJsonVinculo(Long unidadeId) {

        return """
                {
                    "unidadeId": %d
                }
                """.formatted(unidadeId);
    }

    // remove exclusivamente os registros criados durante cada teste
    @AfterEach
    void limparDadosDeTeste() {

        // evita consultas desnecessárias quando não existem registros
        if (emailsUsuariosTeste.isEmpty()
                && cnpjsUnidadesTeste.isEmpty() && categoriasEstoqueTeste.isEmpty()) {
            return;
        }

        // confirma o ambiente antes de realizar qualquer exclusão
        verificarAmbienteDeTestes();

        // Remove primeiro o histórico e os produtos de estoque criados pelos testes.
        for (String categoria : categoriasEstoqueTeste) {
            jdbcTemplate.update("""
                    DELETE m FROM movimentacao_estoque m
                    JOIN produto_unidade pu ON pu.id = m.produto_unidade_id
                    JOIN produto p ON p.id = pu.produto_id
                    JOIN categoria c ON c.id = p.categoria_id
                    WHERE c.nome = ?
                    """, categoria);
            jdbcTemplate.update("""
                    DELETE pu FROM produto_unidade pu
                    JOIN produto p ON p.id = pu.produto_id
                    JOIN categoria c ON c.id = p.categoria_id
                    WHERE c.nome = ?
                    """, categoria);
            jdbcTemplate.update("""
                    DELETE p FROM produto p
                    JOIN categoria c ON c.id = p.categoria_id
                    WHERE c.nome = ?
                    """, categoria);
            jdbcTemplate.update("DELETE FROM categoria WHERE nome = ?", categoria);
        }
        categoriasEstoqueTeste.clear();

        // remove os registros respeitando as chaves estrangeiras
        for (String email : emailsUsuariosTeste) {
            // remove somente eventos associados ao usuário fictício
            jdbcTemplate.update("""
                    DELETE a FROM auditoria a
                    JOIN usuario u ON u.id = a.usuario_id
                    WHERE u.email = ?
                    """, email);

            // remove os vínculos do usuário com as unidades
            jdbcTemplate.update("""
                    DELETE uu
                    FROM usuario_unidade uu
                    INNER JOIN usuario u
                        ON u.id = uu.usuario_id
                    WHERE u.email = ?
                    """,
                    email
            );

            // remove os perfis associados ao usuário
            jdbcTemplate.update("""
                    DELETE up
                    FROM usuario_perfil up
                    INNER JOIN usuario u
                        ON u.id = up.usuario_id
                    WHERE u.email = ?
                    """,
                    email
            );

            // remove o usuário temporário
            jdbcTemplate.update(
                    "DELETE FROM usuario WHERE email = ?",
                    email
            );
        }

        // remove exclusivamente as unidades temporárias
        for (String cnpj : cnpjsUnidadesTeste) {

            jdbcTemplate.update(
                    "DELETE FROM unidade WHERE cnpj = ?",
                    cnpj
            );
        }

        // limpa as listas auxiliares
        emailsUsuariosTeste.clear();
        cnpjsUnidadesTeste.clear();
    }

    // teste 01: verifica o cadastro administrativo de um atendente
    @Test
    void deveCadastrarAtendenteComTokenAdministrativo() throws Exception {

        // autentica um administrador temporário
        String token = autenticarAdministrador();

        // prepara os dados do funcionário
        String email = gerarEmailTeste();

        String json = criarJsonFuncionario(
                "Atendente Teste",
                email,
                "ATENDENTE"
        );

        // executa o cadastro administrativo
        mockMvc.perform(
                        post("/api/usuarios/funcionarios")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Atendente Teste"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.perfis.length()").value(1))
                .andExpect(jsonPath("$.perfis[0]").value("ATENDENTE"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        // consulta a senha efetivamente armazenada no banco
        String senhaHash = jdbcTemplate.queryForObject(
                "SELECT senha_hash FROM usuario WHERE email = ?",
                String.class,
                email
        );

        // garante que a senha foi armazenada de forma codificada
        assertNotEquals(SENHA_TESTE, senhaHash);
        assertTrue(passwordEncoder.matches(SENHA_TESTE, senhaHash));

        // consulta os perfis efetivamente associados ao funcionário
        List<String> perfis = jdbcTemplate.queryForList("""
                SELECT p.nome
                FROM perfil p
                INNER JOIN usuario_perfil up
                    ON up.perfil_id = p.id
                INNER JOIN usuario u
                    ON u.id = up.usuario_id
                WHERE u.email = ?
                """,
                String.class,
                email
        );

        // confirma que somente o perfil autorizado foi persistido
        assertEquals(List.of("ATENDENTE"), perfis);
    }

    // teste 02: verifica o cadastro administrativo de um gerente
    @Test
    void deveCadastrarGerenteComTokenAdministrativo() throws Exception {

        String token = autenticarAdministrador();
        String email = gerarEmailTeste();

        // prepara os dados do gerente
        String json = criarJsonFuncionario(
                "Gerente Teste",
                email,
                "GERENTE"
        );

        // executa o cadastro e valida os dados retornados
        mockMvc.perform(
                        post("/api/usuarios/funcionarios")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Gerente Teste"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.perfis.length()").value(1))
                .andExpect(jsonPath("$.perfis[0]").value("GERENTE"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        // consulta os perfis persistidos no banco
        List<String> perfis = jdbcTemplate.queryForList("""
                SELECT p.nome
                FROM perfil p
                INNER JOIN usuario_perfil up
                    ON up.perfil_id = p.id
                INNER JOIN usuario u
                    ON u.id = up.usuario_id
                WHERE u.email = ?
                """,
                String.class,
                email
        );

        // confirma que o usuário possui exclusivamente o perfil GERENTE
        assertEquals(List.of("GERENTE"), perfis);
    }

    // teste 03: verifica a listagem e a consulta administrativa
    @Test
    void deveListarEConsultarUsuariosComoAdministrador() throws Exception {

        String token = autenticarAdministrador();

        // cria um funcionário temporário para consulta
        String email = criarUsuarioTeste("ATENDENTE", true);

        Long usuarioId = buscarUsuarioId(email);

        // executa a listagem administrativa de usuários
        String resposta = mockMvc.perform(
                        get("/api/usuarios")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // extrai os e-mails presentes na listagem
        List<String> emails = JsonPath.read(resposta, "$[*].email");

        // confirma que o funcionário está presente na respost
        assertTrue(emails.contains(email));

        // garante que os hashes de senha não aparecem na listagem
        List<Object> hashes = JsonPath.read(resposta, "$[*].senhaHash");
        assertTrue(hashes.isEmpty());

        // consulta o funcionário pelo identificador
        mockMvc.perform(
                        get("/api/usuarios/{id}", usuarioId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioId))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.perfis[0]").value("ATENDENTE"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    // teste 04: impede o acesso administrativo sem autenticação
    @Test
    void deveNegarAcessoAdministrativoSemToken() throws Exception {

        // prepara um e-mail exclusivo para a tentativa de cadastro
        String email = gerarEmailTeste();

        String json = criarJsonFuncionario(
                "Atendente Teste",
                email,
                "ATENDENTE"
        );

        // verifica se a listagem exige autenticação
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // erifica se a consulta individual exige autenticação
        mockMvc.perform(get("/api/usuarios/{id}", 1L))
                .andExpect(status().isUnauthorized());

        // verifica se o cadastro administrativo exige autenticação
        mockMvc.perform(
                        post("/api/usuarios/funcionarios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnauthorized());

        // verifica se a listagem de vínculos exige autenticação
        mockMvc.perform(get("/api/usuarios/{id}/unidades", 1L))
                .andExpect(status().isUnauthorized());

        // verifica se a criação de vínculos exige autenticação
        mockMvc.perform(
                        post("/api/usuarios/{id}/unidades", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(criarJsonVinculo(1L))
                )
                .andExpect(status().isUnauthorized());

        // verifica se a desvinculação exige autenticação
        mockMvc.perform(
                        delete(
                                "/api/usuarios/{id}/unidades/{unidadeId}",
                                1L,
                                1L
                        )
                )
                .andExpect(status().isUnauthorized());

        // confirma que a tentativa de cadastro não gerou registros
        Long quantidade = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE email = ?",
                Long.class,
                email
        );

        assertEquals(0L, quantidade);
    }

    // teste 05: impede operações administrativas por outros perfis
    @Test
    void deveNegarAcessoAdministrativoPorPerfisNaoAutorizados()
            throws Exception {

        // define todos os perfis sem autorização administrativa
        List<String> perfis = List.of(
                "CLIENTE",
                "ATENDENTE",
                "GERENTE"
        );

        // cria um funcionário que será utilizado nas tentativas de acesso
        String emailFuncionario = criarUsuarioTeste("ATENDENTE", true);

        Long usuarioId = buscarUsuarioId(emailFuncionario);

        // cria uma unidade temporária para as tentativas de vinculação
        Long unidadeId = criarUnidadeTeste(true);

        // prepara um cadastro que deverá ser rejeitado
        String emailNovoFuncionario = gerarEmailTeste();

        String json = criarJsonFuncionario(
                "Funcionário Não Autorizado",
                emailNovoFuncionario,
                "GERENTE"
        );

        // executa os mesmos cenários para cada perfil não autorizado
        for (String perfil : perfis) {

            // cria e autentica um usuário com o perfil correspondente
            String email = criarUsuarioTeste(perfil, true);

            String token = autenticarUsuario(email);

            // impede a listagem administrativa de usuários
            mockMvc.perform(
                            get("/api/usuarios")
                                    .header("Authorization", "Bearer " + token)
                    )
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            // impede a consulta administrativa de outro usuário
            mockMvc.perform(
                            get("/api/usuarios/{id}", usuarioId)
                                    .header("Authorization", "Bearer " + token)
                    )
                    .andExpect(status().isForbidden());

            // impede o cadastro administrativo de funcionários
            mockMvc.perform(
                            post("/api/usuarios/funcionarios")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isForbidden());

            // impede a consulta administrativa de vínculos
            mockMvc.perform(
                            get("/api/usuarios/{id}/unidades", usuarioId)
                                    .header("Authorization", "Bearer " + token)
                    )
                    .andExpect(status().isForbidden());

            // impede a criação de vínculos sem autorização administrativa
            mockMvc.perform(
                            post("/api/usuarios/{id}/unidades", usuarioId)
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(criarJsonVinculo(unidadeId))
                    )
                    .andExpect(status().isForbidden());

            // impede a desvinculação sem autorização administrativa
            mockMvc.perform(
                            delete(
                                    "/api/usuarios/{id}/unidades/{unidadeId}",
                                    usuarioId,
                                    unidadeId
                            )
                                    .header("Authorization", "Bearer " + token)
                    )
                    .andExpect(status().isForbidden());
        }

        // confirma que nenhum funcionário foi cadastrado indevidamente
        Long quantidadeUsuarios = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE email = ?",
                Long.class,
                emailNovoFuncionario
        );

        assertEquals(0L, quantidadeUsuarios);

        // confirma que nenhum vínculo foi criado indevidamente
        Long quantidadeVinculos = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario_unidade
            WHERE usuario_id = ?
              AND unidade_id = ?
            """,
                Long.class,
                usuarioId,
                unidadeId
        );

        assertEquals(0L, quantidadeVinculos);
    }

    // teste 06: impede o cadastro de funcionários com perfis proibidos
    @Test
    void deveRejeitarCadastroAdministrativoComPerfilProibido()
            throws Exception {

        // autentica um administrador autorizado
        String token = autenticarAdministrador();

        // define os perfis que não podem ser utilizados nesse cadastro
        List<String> perfisProibidos = List.of(
                "ADMIN",
                "CLIENTE",
                "INEXISTENTE"
        );

        // verifica individualmente cada perfil proibido
        for (String perfil : perfisProibidos) {

            // gera um e-mail exclusivo para cada tentativa
            String email = gerarEmailTeste();

            String json = criarJsonFuncionario(
                    "Funcionário Teste",
                    email,
                    perfil
            );

            // executa a tentativa de cadastro com perfil inválido
            mockMvc.perform(
                            post("/api/usuarios/funcionarios")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            // confirma que o usuário não foi persistido
            Long quantidade = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM usuario WHERE email = ?",
                    Long.class,
                    email
            );

            assertEquals(0L, quantidade);
        }
    }

    // teste 07: verifica a criação e a persistência do vínculo de um atendente
    @Test
    void deveVincularAtendenteAUnidade() throws Exception {

        // autentica um administrador autorizado
        String token = autenticarAdministrador();

        // cria um atendente temporário
        String email = criarUsuarioTeste("ATENDENTE", true);

        Long usuarioId = buscarUsuarioId(email);

        // cria uma unidade ativa para a vinculação
        Long unidadeId = criarUnidadeTeste(true);

        // executa a vinculação pelo endpoint administrativo
        mockMvc.perform(
                        post("/api/usuarios/{id}/unidades", usuarioId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(criarJsonVinculo(unidadeId))
                )
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.usuarioId").value(usuarioId))
                .andExpect(jsonPath("$.unidadeId").value(unidadeId))
                .andExpect(jsonPath("$.usuarioNome")
                        .value("Usuário Administração Teste"))
                .andExpect(jsonPath("$.unidadeNome")
                        .value("Unidade Administração Teste"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.dataVinculo").isNotEmpty());

        // consulta o vínculo efetivamente persistido no MySQL
        Long quantidade = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario_unidade
            WHERE usuario_id = ?
              AND unidade_id = ?
              AND ativo = 1
              AND data_vinculo IS NOT NULL
            """,
                Long.class,
                usuarioId,
                unidadeId
        );

        // confirma a persistência de exatamente um vínculo ativo
        assertEquals(1L, quantidade);

        // consulta os vínculos pelo endpoint administrativo
        mockMvc.perform(
                        get("/api/usuarios/{id}/unidades", usuarioId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].usuarioId").value(usuarioId))
                .andExpect(jsonPath("$[0].unidadeId").value(unidadeId))
                .andExpect(jsonPath("$[0].ativo").value(true));

        // confirma que o vínculo permite acesso à unidade
        assertDoesNotThrow(() ->
                permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId
                )
        );
    }

    // teste 08: impede que um atendente possua mais de um vínculo ativo
    @Test
    void deveRejeitarSegundoVinculoAtivoDeAtendente()
            throws Exception {

        // autentica um administrador autorizado
        String token = autenticarAdministrador();

        // cria um atendente temporário
        String email = criarUsuarioTeste("ATENDENTE", true);

        Long usuarioId = buscarUsuarioId(email);

        // cria duas unidades ativas e independentes
        Long unidadeId1 = criarUnidadeTeste(true);
        Long unidadeId2 = criarUnidadeTeste(true);

        // realiza a primeira vinculação
        mockMvc.perform(
                        post("/api/usuarios/{id}/unidades", usuarioId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(criarJsonVinculo(unidadeId1))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        // tenta vincular o mesmo atendente a uma segunda unidade
        mockMvc.perform(
                        post("/api/usuarios/{id}/unidades", usuarioId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(criarJsonVinculo(unidadeId2))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value(
                        "O atendente já possui vínculo ativo com outra unidade."
                ));

        // tenta duplicar o vínculo com a primeira unidade
        mockMvc.perform(
                        post("/api/usuarios/{id}/unidades", usuarioId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(criarJsonVinculo(unidadeId1))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value(
                        "O usuário já possui vínculo ativo com esta unidade."
                ));

        // consulta a quantidade de vínculos ativos do atendente
        Long quantidadeAtivos = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario_unidade
            WHERE usuario_id = ?
              AND ativo = 1
            """,
                Long.class,
                usuarioId
        );

        // confirma que apenas o primeiro vínculo permanece ativo
        assertEquals(1L, quantidadeAtivos);

        // verifica se a tentativa rejeitada gerou algum registro
        Long quantidadeSegundoVinculo = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario_unidade
            WHERE usuario_id = ?
              AND unidade_id = ?
            """,
                Long.class,
                usuarioId,
                unidadeId2
        );

        // garante que a segunda associação não foi persistida
        assertEquals(0L, quantidadeSegundoVinculo);

        // confirma que o atendente possui acesso à primeira unidade
        assertDoesNotThrow(() ->
                permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId1
                )
        );

        // confirma que o atendente não possui acesso à segunda unidade
        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId2
                )
        );
    }

    // teste 09: permite que um gerente possua múltiplos vínculos ativos
    @Test
    void devePermitirMultiplosVinculosAtivosDeGerente()
            throws Exception {

        // autentica um administrador autorizado
        String token = autenticarAdministrador();

        // cria um gerente temporário
        String email = criarUsuarioTeste("GERENTE", true);

        Long usuarioId = buscarUsuarioId(email);

        // cria três unidades ativas
        Long unidadeId1 = criarUnidadeTeste(true);
        Long unidadeId2 = criarUnidadeTeste(true);
        Long unidadeId3 = criarUnidadeTeste(true);

        // define as duas unidades que serão vinculadas ao gerente
        List<Long> unidadesAutorizadas = List.of(
                unidadeId1,
                unidadeId2
        );

        // realiza a vinculação com cada unidade autorizada
        for (Long unidadeId : unidadesAutorizadas) {

            mockMvc.perform(
                            post("/api/usuarios/{id}/unidades", usuarioId)
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(criarJsonVinculo(unidadeId))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuarioId").value(usuarioId))
                    .andExpect(jsonPath("$.unidadeId").value(unidadeId))
                    .andExpect(jsonPath("$.ativo").value(true))
                    .andExpect(jsonPath("$.dataVinculo").isNotEmpty());
        }

        // consulta os identificadores das unidades vinculadas no MySQL
        List<Long> unidadesPersistidas = jdbcTemplate.queryForList("""
            SELECT unidade_id
            FROM usuario_unidade
            WHERE usuario_id = ?
              AND ativo = 1
            """,
                Long.class,
                usuarioId
        );

        // confirma a persistência dos dois vínculos ativos
        assertEquals(2, unidadesPersistidas.size());
        assertTrue(unidadesPersistidas.containsAll(unidadesAutorizadas));

        // consulta os vínculos pelo endpoint administrativo
        mockMvc.perform(
                        get("/api/usuarios/{id}/unidades", usuarioId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // confirma que o gerente possui acesso às duas unidades vinculadas
        for (Long unidadeId : unidadesAutorizadas) {

            assertDoesNotThrow(() ->
                    permissaoUnidadeService.validarAcesso(
                            email,
                            unidadeId
                    )
            );
        }

        // confirma que o gerente não possui acesso à terceira unidade
        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId3
                )
        );
    }

    // teste 10: verifica a desativação e a reativação de um vínculo
    @Test
    void deveDesvincularEReativarAtendente() throws Exception {

        // autentica um administrador autorizado
        String token = autenticarAdministrador();

        // cria um atendente e uma unidade temporária
        String email = criarUsuarioTeste("ATENDENTE", true);

        Long usuarioId = buscarUsuarioId(email);
        Long unidadeId = criarUnidadeTeste(true);

        // cria o vínculo inicial
        mockMvc.perform(
                        post("/api/usuarios/{id}/unidades", usuarioId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(criarJsonVinculo(unidadeId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        // recupera a data original do vínculo persistido
        LocalDateTime dataOriginal = jdbcTemplate.queryForObject("""
            SELECT data_vinculo
            FROM usuario_unidade
            WHERE usuario_id = ?
              AND unidade_id = ?
            """,
                LocalDateTime.class,
                usuarioId,
                unidadeId
        );

        assertNotNull(dataOriginal);

        // desativa o vínculo pelo endpoint administrativo
        mockMvc.perform(
                        delete(
                                "/api/usuarios/{id}/unidades/{unidadeId}",
                                usuarioId,
                                unidadeId
                        )
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        // confirma que o registro permanece no banco, porém inativo
        Long quantidadeInativos = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario_unidade
            WHERE usuario_id = ?
              AND unidade_id = ?
              AND ativo = 0
            """,
                Long.class,
                usuarioId,
                unidadeId
        );

        assertEquals(1L, quantidadeInativos);

        // impede uma segunda desvinculação do mesmo registro
        mockMvc.perform(
                        delete(
                                "/api/usuarios/{id}/unidades/{unidadeId}",
                                usuarioId,
                                unidadeId
                        )
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value("O vínculo já está inativo."));

        // reativa o vínculo anteriormente desativado
        mockMvc.perform(
                        post("/api/usuarios/{id}/unidades", usuarioId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(criarJsonVinculo(unidadeId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        // recupera a data do vínculo após a reativação
        LocalDateTime dataReativacao = jdbcTemplate.queryForObject("""
            SELECT data_vinculo
            FROM usuario_unidade
            WHERE usuario_id = ?
              AND unidade_id = ?
            """,
                LocalDateTime.class,
                usuarioId,
                unidadeId
        );

        // garante que a data original foi preservada
        assertEquals(dataOriginal, dataReativacao);

        // confirma que existe somente um registro para a associação
        Long quantidadeTotal = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario_unidade
            WHERE usuario_id = ?
              AND unidade_id = ?
            """,
                Long.class,
                usuarioId,
                unidadeId
        );

        assertEquals(1L, quantidadeTotal);

        // confirma que o vínculo reativado permite acesso novamente
        assertDoesNotThrow(() ->
                permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId
                )
        );
    }

    // teste 11: verifica a atualização das permissões após alterações no banco
    @Test
    void deveAtualizarPermissoesAposDesvinculacao() throws Exception {

        // autentica um administrador autorizado
        String token = autenticarAdministrador();

        // cria um atendente e duas unidades temporárias
        String email = criarUsuarioTeste("ATENDENTE", true);

        Long usuarioId = buscarUsuarioId(email);

        Long unidadeId1 = criarUnidadeTeste(true);
        Long unidadeId2 = criarUnidadeTeste(true);

        // vincula o atendente à primeira unidade
        mockMvc.perform(
                        post("/api/usuarios/{id}/unidades", usuarioId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(criarJsonVinculo(unidadeId1))
                )
                .andExpect(status().isOk());

        // confirma a autorização inicial na primeira unidade
        assertDoesNotThrow(() ->
                permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId1
                )
        );

        // confirma que a segunda unidade ainda não está autorizada
        assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId2
                )
        );

        // desativa o vínculo com a primeira unidade
        mockMvc.perform(
                        delete(
                                "/api/usuarios/{id}/unidades/{unidadeId}",
                                usuarioId,
                                unidadeId1
                        )
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        // confirma que o acesso anterior foi revogado
        assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId1
                )
        );

        // vincula o atendente à segunda unidade
        mockMvc.perform(
                        post("/api/usuarios/{id}/unidades", usuarioId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(criarJsonVinculo(unidadeId2))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        // confirma que o atendente possui acesso à nova unidade
        assertDoesNotThrow(() ->
                permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId2
                )
        );

        // garante que a unidade anterior continua sem autorização
        assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId1
                )
        );

        // confirma que somente um vínculo permanece ativo
        Long quantidadeAtivos = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario_unidade
            WHERE usuario_id = ?
              AND ativo = 1
            """,
                Long.class,
                usuarioId
        );

        assertEquals(1L, quantidadeAtivos);

        // desativa o funcionário diretamente no banco de testes
        jdbcTemplate.update(
                "UPDATE usuario SET ativo = 0 WHERE id = ?",
                usuarioId
        );

        // confirma que um vínculo ativo não autoriza um usuário inativo
        assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        email,
                        unidadeId2
                )
        );
    }

    // teste 12: impede múltiplos vínculos ativos em operações concorrentes
    @Test
    void deveImpedirVinculosConcorrentesDeAtendente()
            throws Exception {

        // autentica o administrador responsável pelas duas operações
        String admin = autenticarAdministrador();

        // cria um atendente temporário sem vínculos anteriores
        String email = criarUsuarioTeste("ATENDENTE", true);

        Long usuarioId = buscarUsuarioId(email);

        // cria duas unidades ativas para as operações concorrentes
        Long unidadeId1 = criarUnidadeTeste(true);
        Long unidadeId2 = criarUnidadeTeste(true);

        // cria um executor capaz de executar duas tarefas simultaneamente
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // controla a preparação das duas tarefas
        CountDownLatch tarefasProntas = new CountDownLatch(2);

        // permite liberar as duas tarefas aproximadamente ao mesmo tempo
        CountDownLatch iniciarOperacoes = new CountDownLatch(1);

        try {

            // prepara a primeira tentativa de vinculação
            Future<Boolean> primeiraOperacao = executor.submit(() -> {

                // informa que a primeira tarefa está pronta
                tarefasProntas.countDown();

                // aguarda a autorização para iniciar
                if (!iniciarOperacoes.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Tempo limite para iniciar a primeira operação."
                    );
                }

                try {

                    // tenta vincular o atendente à primeira unidade
                    var respostaVinculo = mockMvc.perform(post("/api/usuarios/{id}/unidades", usuarioId)
                                    .header("Authorization", "Bearer " + admin)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(criarJsonVinculo(unidadeId1)))
                            .andReturn().getResponse();
                    int statusHttp = respostaVinculo.getStatus();
                    // confirma que somente sucesso ou conflito são aceitos
                    assertTrue(statusHttp == 200 || statusHttp == 409);
                    if (statusHttp == 409) {
                        assertEquals("O atendente já possui vínculo ativo com outra unidade.",
                                JsonPath.read(respostaVinculo.getContentAsString(), "$.mensagem"));
                        return false;
                    }

                    return true;

                } catch (IllegalArgumentException excecao) {

                    // aceita somente a rejeição prevista pela regra de negócio
                    assertEquals(
                            "O atendente já possui vínculo ativo com outra unidade.",
                            excecao.getMessage()
                    );

                    return false;
                }
            });

            // prepara a segunda tentativa de vinculação
            Future<Boolean> segundaOperacao = executor.submit(() -> {

                // informa que a segunda tarefa está pronta
                tarefasProntas.countDown();

                // aguarda a autorização para iniciar
                if (!iniciarOperacoes.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Tempo limite para iniciar a segunda operação."
                    );
                }

                try {

                    // tenta vincular o atendente à segunda unidade
                    var respostaVinculo = mockMvc.perform(post("/api/usuarios/{id}/unidades", usuarioId)
                                    .header("Authorization", "Bearer " + admin)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(criarJsonVinculo(unidadeId2)))
                            .andReturn().getResponse();
                    int statusHttp = respostaVinculo.getStatus();
                    // confirma que somente sucesso ou conflito são aceitos
                    assertTrue(statusHttp == 200 || statusHttp == 409);
                    if (statusHttp == 409) {
                        assertEquals("O atendente já possui vínculo ativo com outra unidade.",
                                JsonPath.read(respostaVinculo.getContentAsString(), "$.mensagem"));
                        return false;
                    }

                    return true;

                } catch (IllegalArgumentException excecao) {

                    // aceita somente a rejeição prevista pela regra de negócio
                    assertEquals(
                            "O atendente já possui vínculo ativo com outra unidade.",
                            excecao.getMessage()
                    );

                    return false;
                }
            });

            // aguarda a preparação das duas tarefas
            assertTrue(
                    tarefasProntas.await(10, TimeUnit.SECONDS),
                    "As duas tarefas deveriam estar prontas."
            );

            // libera as duas operações concorrentes
            iniciarOperacoes.countDown();

            // recupera os resultados com limite de tempo
            boolean primeiroResultado =
                    primeiraOperacao.get(30, TimeUnit.SECONDS);

            boolean segundoResultado =
                    segundaOperacao.get(30, TimeUnit.SECONDS);

            // garante que exatamente uma operação foi bem-sucedida
            assertNotEquals(
                    primeiroResultado,
                    segundoResultado,
                    "Somente uma vinculação deveria ser permitida."
            );

            // consulta a quantidade de vínculos ativos no banco
            Long quantidadeAtivos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM usuario_unidade
                WHERE usuario_id = ?
                  AND ativo = 1
                """,
                    Long.class,
                    usuarioId
            );

            // confirma que a regra foi preservada no MySQL
            assertEquals(1L, quantidadeAtivos);

            // consulta a quantidade total de vínculos persistidos
            Long quantidadeTotal = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM usuario_unidade
                WHERE usuario_id = ?
                """,
                    Long.class,
                    usuarioId
            );

            // garante que a operação rejeitada não deixou registros
            assertEquals(1L, quantidadeTotal);

        } finally {

            // libera eventuais tarefas que ainda estejam aguardando
            iniciarOperacoes.countDown();

            // encerra o executor mesmo quando ocorre uma falha
            executor.shutdownNow();

            // aguarda o encerramento antes da limpeza dos registros
            assertTrue(
                    executor.awaitTermination(30, TimeUnit.SECONDS),
                    "As tarefas concorrentes deveriam ter sido encerradas."
            );
        }
    }

    // cria um produto com saldo inicial em uma unidade temporária
    private Long criarEstoqueTeste(Long unidadeId) {
        verificarAmbienteDeTestes();
        String categoria = "Estoque " + UUID.randomUUID();
        categoriasEstoqueTeste.add(categoria);
        jdbcTemplate.update("INSERT INTO categoria (nome, ativa) VALUES (?, true)", categoria);
        Long categoriaId = jdbcTemplate.queryForObject(
                "SELECT id FROM categoria WHERE nome = ?", Long.class, categoria);
        jdbcTemplate.update("INSERT INTO produto (categoria_id, nome, ativo) VALUES (?, ?, true)",
                categoriaId, "Produto de estoque teste");
        Long produtoId = jdbcTemplate.queryForObject(
                "SELECT id FROM produto WHERE categoria_id = ?", Long.class, categoriaId);
        jdbcTemplate.update("""
                INSERT INTO produto_unidade (produto_id, unidade_id, preco, estoque, disponivel)
                VALUES (?, ?, 12.90, 50, true)
                """, produtoId, unidadeId);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM produto_unidade WHERE produto_id = ? AND unidade_id = ?
                """, Long.class, produtoId, unidadeId);
    }

    // prepara uma entrada válida para testar a autorização, não a validação do DTO
    private String criarJsonEntrada(Long estoqueId) {
        return """
                {"produtoUnidadeId": %d, "tipo": "ENTRADA", "quantidade": 10,
                 "motivo": "Teste de autorização"}
                """.formatted(estoqueId);
    }

    // teste 13: administrador global e gerente limitado às unidades vinculadas
    @Test
    void deveRespeitarPermissoesDeAdministradorEGerenteNoEstoque() throws Exception {
        String admin = autenticarAdministrador();
        String email = criarUsuarioTeste("GERENTE", true);
        Long unidadeA = criarUnidadeTeste(true);
        Long unidadeB = criarUnidadeTeste(true);
        Long estoqueA = criarEstoqueTeste(unidadeA);
        Long estoqueB = criarEstoqueTeste(unidadeB);
        // realiza o vínculo com a identidade administrativa real
        mockMvc.perform(post("/api/usuarios/{id}/unidades", buscarUsuarioId(email))
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarJsonVinculo(unidadeA)))
                .andExpect(status().isOk());
        String gerente = autenticarUsuario(email);

        // o administrador pode consultar o histórico global
        mockMvc.perform(get("/api/movimentacoes-estoque").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        // o gerente pode movimentar somente o estoque autorizado
        String resposta = mockMvc.perform(post("/api/movimentacoes-estoque")
                        .header("Authorization", "Bearer " + gerente)
                        .contentType(MediaType.APPLICATION_JSON).content(criarJsonEntrada(estoqueA)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.unidadeId").value(unidadeA))
                .andReturn().getResponse().getContentAsString();
        Number movimentoId = JsonPath.read(resposta, "$.id");
        mockMvc.perform(get("/api/movimentacoes-estoque/{id}", movimentoId.longValue())
                .header("Authorization", "Bearer " + gerente)).andExpect(status().isOk());
        mockMvc.perform(post("/api/movimentacoes-estoque").header("Authorization", "Bearer " + gerente)
                        .contentType(MediaType.APPLICATION_JSON).content(criarJsonEntrada(estoqueB)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/movimentacoes-estoque").header("Authorization", "Bearer " + gerente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/movimentacoes-estoque/tipo").param("tipo", "ENTRADA")
                .header("Authorization", "Bearer " + gerente)).andExpect(status().isForbidden());
        // Confirma que a operação rejeitada não alterou o saldo da outra unidade.
        assertEquals(60, jdbcTemplate.queryForObject("SELECT estoque FROM produto_unidade WHERE id = ?", Integer.class, estoqueA));
        assertEquals(50, jdbcTemplate.queryForObject("SELECT estoque FROM produto_unidade WHERE id = ?", Integer.class, estoqueB));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movimentacao_estoque WHERE produto_unidade_id = ?", Long.class, estoqueB));
    }

    // teste 14: atendente pode consultar sua unidade; cliente não acessa estoque
    @Test
    void deveRespeitarRestricoesDeAtendenteEClienteNoEstoque() throws Exception {
        String admin = autenticarAdministrador();
        String email = criarUsuarioTeste("ATENDENTE", true);
        Long unidadeA = criarUnidadeTeste(true);
        Long unidadeB = criarUnidadeTeste(true);
        Long estoqueA = criarEstoqueTeste(unidadeA);
        Long estoqueB = criarEstoqueTeste(unidadeB);
        // realiza o vínculo com a identidade administrativa real
        mockMvc.perform(post("/api/usuarios/{id}/unidades", buscarUsuarioId(email))
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarJsonVinculo(unidadeA)))
                .andExpect(status().isOk());
        String atendente = autenticarUsuario(email);
        String cliente = autenticarUsuario(criarUsuarioTeste("CLIENTE", true));
        // Cria um histórico real na unidade não autorizada para testar acesso por ID.
        String resposta = mockMvc.perform(post("/api/movimentacoes-estoque")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(criarJsonEntrada(estoqueB)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number movimentoId = JsonPath.read(resposta, "$.id");
        mockMvc.perform(get("/api/movimentacoes-estoque/unidade/{id}", unidadeA)
                .header("Authorization", "Bearer " + atendente)).andExpect(status().isOk());
        mockMvc.perform(get("/api/movimentacoes-estoque/unidade/{id}", unidadeB)
                .header("Authorization", "Bearer " + atendente)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/movimentacoes-estoque/produto-unidade/{id}", estoqueB)
                .header("Authorization", "Bearer " + atendente)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/movimentacoes-estoque/{id}", movimentoId.longValue())
                .header("Authorization", "Bearer " + atendente)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/movimentacoes-estoque").header("Authorization", "Bearer " + atendente)
                        .contentType(MediaType.APPLICATION_JSON).content(criarJsonEntrada(estoqueA)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/movimentacoes-estoque/unidade/{id}", unidadeA)
                .header("Authorization", "Bearer " + cliente)).andExpect(status().isForbidden());
        assertEquals(50, jdbcTemplate.queryForObject("SELECT estoque FROM produto_unidade WHERE id = ?", Integer.class, estoqueA));
    }

    // teste 15: o mesmo JWT perde acesso quando o vínculo é desativado
    @Test
    void deveNegarEstoqueComMesmoTokenAposDesvinculacao() throws Exception {
        String admin = autenticarAdministrador();
        String email = criarUsuarioTeste("ATENDENTE", true);
        Long usuarioId = buscarUsuarioId(email);
        Long unidadeId = criarUnidadeTeste(true);
        // realiza o vínculo com a identidade administrativa real
        mockMvc.perform(post("/api/usuarios/{id}/unidades", usuarioId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(criarJsonVinculo(unidadeId)))
                .andExpect(status().isOk());
        String token = autenticarUsuario(email);
        // Confirma acesso antes da revogação.
        mockMvc.perform(get("/api/movimentacoes-estoque/unidade/{id}", unidadeId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/usuarios/{id}/unidades/{unidadeId}", usuarioId, unidadeId)
                .header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        // Reutiliza exatamente o token emitido antes da desvinculação.
        mockMvc.perform(get("/api/movimentacoes-estoque/unidade/{id}", unidadeId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }
}