package br.com.raizesnordeste.raizesnordeste.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Locale;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


// carrega o contexto completo da aplicação Spring Boot
@SpringBootTest

// configura o MockMvc para simular requisições HTTP
@AutoConfigureMockMvc

// ativa exclusivamente o perfil de configuração de testes
@ActiveProfiles("test")
class UsuarioIntegrationTest {

    // permite realizar requisições HTTP sem iniciar um servidor externo
    @Autowired
    private MockMvc mockMvc;

    // permite consultar e manipular os dados do banco de testes
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // utiliza o codificador real de senhas configurado na aplicação
    @Autowired
    private PasswordEncoder passwordEncoder;

    // senha fictícia utilizada exclusivamente nos testes de integração
    private static final String SENHA_TESTE = "SenhaCadastro@2026!";

    // armazena os e-mails utilizados durante cada teste
    private final List<String> emailsUsuariosTeste = new ArrayList<>();

    // verifica o ambiente antes da execução de cada teste
    @BeforeEach
    void verificarAmbienteDeTestes() {

        // consulta o nome do banco de dados utilizado
        String bancoAtual = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        // garante que o banco exclusivo de testes está sendo utilizado
        assertEquals(
                "raizes_nordeste_test",
                bancoAtual,
                "Os testes devem utilizar exclusivamente o banco de testes."
        );

        // consulta a conta MySQL utilizada pela aplicação
        String usuarioAtual = jdbcTemplate.queryForObject(
                "SELECT CURRENT_USER()",
                String.class
        );

        // garante que a conta exclusiva de testes está sendo utilizada
        assertEquals(
                "raizes_test@localhost",
                usuarioAtual,
                "Os testes devem utilizar exclusivamente o usuario de testes."
        );
    }

    // gera um e-mail único para evitar conflitos entre os testes
    private String gerarEmailTeste() {

        // utiliza um UUID para criar um identificador exclusivo
        String email = "cadastro.teste."
                + UUID.randomUUID()
                + "@raizes.local";

        // registra o e-mail para permitir a limpeza posterior
        emailsUsuariosTeste.add(email);

        // retorna o e-mail que será utilizado na requisição
        return email;
    }

    // monta o corpo JSON utilizado nas requisições de cadastro
    private String criarJsonCadastro(String nome, String email, String senha) {

        return """
            {
                "nome": "%s",
                "email": "%s",
                "senha": "%s"
            }
            """.formatted(nome, email, senha);
    }

    // executa a limpeza dos dados após cada teste, inclusive quando alguma verificação falhar
    @AfterEach
    void limparDadosDeTeste() {

        // evita operações desnecessárias quando nenhum e-mail foi registrado
        if (emailsUsuariosTeste.isEmpty()) {
            return;
        }

        // confirma novamente que estamos no ambiente de testes
        verificarAmbienteDeTestes();

        // percorre exclusivamente os e-mails utilizados pelo teste
        for (String email : emailsUsuariosTeste) {
            // remove somente eventos associados ao usuário fictício
            jdbcTemplate.update("""
                    DELETE a FROM auditoria a
                    JOIN usuario u ON u.id = a.usuario_id
                    WHERE u.email = ?
                    """, email);

            // remove primeiro os relacionamentos entre usuários e perfis
            jdbcTemplate.update("""
                    DELETE up
                    FROM usuario_perfil up
                    INNER JOIN usuario u ON u.id = up.usuario_id
                    WHERE u.email = ?
                    """, email);

            // remove o usuário fictício identificado pelo e-mail
            jdbcTemplate.update("""
                    DELETE FROM usuario
                    WHERE email = ?
                    """, email);
        }

        // limpa a lista de controle após a exclusão dos registros
        emailsUsuariosTeste.clear();
    }

    // verifica se um cliente pode realizar o cadastro sem autenticação
    @Test
    void deveCadastrarClienteComSucesso() throws Exception {

        // gera um e-mail exclusivo para o teste
        String email = gerarEmailTeste();

        // monta os dados enviados na requisição
        String corpoRequisicao = criarJsonCadastro(
                "Cliente Teste Integracao",
                email,
                SENHA_TESTE
        );

        // realiza o cadastro sem enviar um token JWT
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))

                // verifica se o cadastro retornou HTTP 201
                .andExpect(status().isCreated())

                // verifica se a resposta não deve ser armazenada em cache
                .andExpect(header().string("Cache-Control", "no-store"))

                // verifica os dados retornados pela API
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Cliente Teste Integracao"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.ativo").value(true))

                // garante que o usuário possui exclusivamente o perfil CLIENTE
                .andExpect(jsonPath("$.perfis.length()").value(1))
                .andExpect(jsonPath("$.perfis[0]").value("CLIENTE"))

                // garante que nenhuma informação de senha foi exposta
                .andExpect(jsonPath("$.senha").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.senhaHash").doesNotHaveJsonPath());
    }
    // verifica se o usuário foi persistido com a senha codificada
    @Test
    void devePersistirUsuarioComSenhaCodificada() throws Exception {

        // gera um e-mail exclusivo para o teste
        String email = gerarEmailTeste();

        // monta os dados do novo cliente
        String corpoRequisicao = criarJsonCadastro(
                "Cliente Teste Persistencia",
                email,
                SENHA_TESTE
        );

        // realiza o cadastro pela API
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))
                .andExpect(status().isCreated());

        // consulta a senha armazenada no banco de dados
        String senhaHash = jdbcTemplate.queryForObject("""
            SELECT senha_hash
            FROM usuario
            WHERE email = ?
            """, String.class, email);

        // garante que a senha não foi armazenada em texto puro
        assertNotEquals(SENHA_TESTE, senhaHash);

        // verifica se o BCrypt reconhece a senha original
        assertTrue(passwordEncoder.matches(SENHA_TESTE, senhaHash));

        // consulta os perfis efetivamente associados ao usuário
        List<String> perfis = jdbcTemplate.queryForList("""
            SELECT p.nome
            FROM perfil p
            INNER JOIN usuario_perfil up ON up.perfil_id = p.id
            INNER JOIN usuario u ON u.id = up.usuario_id
            WHERE u.email = ?
            """, String.class, email);

        // garante que somente o perfil CLIENTE foi persistido
        assertEquals(List.of("CLIENTE"), perfis);
    }

    // verifica se a API impede o cadastro de usuários com e-mails duplicados
    @Test
    void deveRejeitarCadastroComEmailDuplicado() throws Exception {

        // gera um e-mail exclusivo para o teste
        String email = gerarEmailTeste();

        // monta os dados do cliente
        String corpoRequisicao = criarJsonCadastro(
                "Cliente Teste Duplicidade",
                email,
                SENHA_TESTE
        );

        // realiza o primeiro cadastro com sucesso
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))
                .andExpect(status().isCreated());

        // tenta realizar outro cadastro utilizando o mesmo e-mail
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))

                // verifica se a API retorna HTTP 409
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem")
                        .value("Já existe um usuário cadastrado com este e-mail."));

        // consulta a quantidade de usuários cadastrados com esse e-mail
        Integer quantidade = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario
            WHERE email = ?
            """, Integer.class, email);

        // garante que apenas um usuário foi persistido
        assertEquals(1, quantidade);
    }

    // verifica se a API rejeita o cadastro com nome em branco
    @Test
    void deveRejeitarCadastroComNomeEmBranco() throws Exception {

        // gera um e-mail exclusivo para o teste
        String email = gerarEmailTeste();

        // monta uma requisição com nome inválido
        String corpoRequisicao = criarJsonCadastro(
                "   ",
                email,
                SENHA_TESTE
        );

        // envia a requisição e verifica o erro de validação
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // garante que nenhum usuário inválido foi persistido
        Integer quantidade = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario
            WHERE email = ?
            """, Integer.class, email);

        assertEquals(0, quantidade);
    }

    // verifica se a API rejeita o cadastro com e-mail inválido
    @Test
    void deveRejeitarCadastroComEmailInvalido() throws Exception {

        // gera um identificador exclusivo sem o formato válido de e-mail
        String email = "email-invalido-" + UUID.randomUUID();

        // registra o identificador para permitir a limpeza posterior
        emailsUsuariosTeste.add(email);

        // monta uma requisição com e-mail inválido
        String corpoRequisicao = criarJsonCadastro(
                "Cliente Teste Email Invalido",
                email,
                SENHA_TESTE
        );

        // envia a requisição e verifica o erro de validação
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // garante que nenhum usuário inválido foi persistido
        Integer quantidade = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario
            WHERE email = ?
            """, Integer.class, email);

        assertEquals(0, quantidade);
    }

    // verifica se a API rejeita o cadastro com senha vazia
    @Test
    void deveRejeitarCadastroComSenhaVazia() throws Exception {

        // gera um e-mail exclusivo para o teste
        String email = gerarEmailTeste();

        // monta uma requisição sem conteúdo no campo senha
        String corpoRequisicao = criarJsonCadastro(
                "Cliente Teste Senha Vazia",
                email,
                ""
        );

        // envia a requisição e verifica o erro de validação
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // garante que nenhum usuário inválido foi persistido
        Integer quantidade = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario
            WHERE email = ?
            """, Integer.class, email);

        assertEquals(0, quantidade);
    }

    // verifica se a API rejeita senhas inferiores ao tamanho mínimo
    @Test
    void deveRejeitarSenhaInferiorAOitoCaracteres() throws Exception {

        // gera um e-mail exclusivo para o teste
        String email = gerarEmailTeste();

        // define uma senha com apenas sete caracteres
        String senha = "Abc1234";

        // monta os dados do cadastro
        String corpoRequisicao = criarJsonCadastro(
                "Cliente Teste Senha Curta",
                email,
                senha
        );

        // envia a requisição e verifica o erro de validação
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // consulta a quantidade de registros associados ao e-mail
        Integer quantidade = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario
            WHERE email = ?
            """, Integer.class, email);

        // garante que nenhum usuário inválido foi persistido
        assertEquals(0, quantidade);
    }

    // verifica se a API rejeita senhas superiores ao tamanho máximo
    @Test
    void deveRejeitarSenhaSuperiorA72Caracteres() throws Exception {

        // gera um e-mail exclusivo para o teste
        String email = gerarEmailTeste();

        // cria uma senha com 73 caracteres
        String senha = "A".repeat(73);

        // monta os dados do cadastro
        String corpoRequisicao = criarJsonCadastro(
                "Cliente Teste Senha Longa",
                email,
                senha
        );

        // envia a requisição e verifica o erro de validação
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // consulta a quantidade de registros associados ao e-mail
        Integer quantidade = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario
            WHERE email = ?
            """, Integer.class, email);

        // garante que nenhum usuário inválido foi persistido
        assertEquals(0, quantidade);
    }

    // verifica se a API rejeita senhas que ultrapassam o limite de bytes do BCrypt
    @Test
    void deveRejeitarSenhaMultibyteSuperiorA72Bytes() throws Exception {

        // gera um e-mail exclusivo para o teste
        String email = gerarEmailTeste();

        // cria uma senha com 37 caracteres que ocupam 74 bytes em UTF-8
        String senha = "á".repeat(37);

        // confirma o tamanho da senha em caracteres e bytes
        assertEquals(37, senha.length());
        assertEquals(74, senha.getBytes(StandardCharsets.UTF_8).length);

        // monta os dados do cadastro
        String corpoRequisicao = criarJsonCadastro(
                "Cliente Teste Senha Multibyte",
                email,
                senha
        );

        // envia a requisição e verifica a regra de negócio
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Requisição inválida"))
                .andExpect(jsonPath("$.mensagem")
                        .value("A senha deve possuir no máximo 72 bytes."));

        // consulta a quantidade de registros associados ao e-mail
        Integer quantidade = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario
            WHERE email = ?
            """, Integer.class, email);

        // garante que nenhum usuário inválido foi persistido
        assertEquals(0, quantidade);
    }

    // verifica se o cadastro público impede a elevação de privilégios
    @Test
    void deveImpedirElevacaoDePrivilegiosNoCadastro() throws Exception {

        // Gera um e-mail exclusivo para o teste.
        String email = gerarEmailTeste();

        // simula uma tentativa de cadastro com privilégios administrativos
        String corpoRequisicao = """
            {
                "nome": "Cliente Teste Privilegios",
                "email": "%s",
                "senha": "%s",
                "perfis": ["ADMIN"],
                "ativo": false
            }
            """.formatted(email, SENHA_TESTE);

        // realiza o cadastro e verifica se os campos indevidos foram ignorados
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.perfis.length()").value(1))
                .andExpect(jsonPath("$.perfis[0]").value("CLIENTE"));

        // consulta os perfis efetivamente persistidos no banco
        List<String> perfis = jdbcTemplate.queryForList("""
            SELECT p.nome
            FROM perfil p
            INNER JOIN usuario_perfil up ON up.perfil_id = p.id
            INNER JOIN usuario u ON u.id = up.usuario_id
            WHERE u.email = ?
            """, String.class, email);

        // garante que nenhum privilégio administrativo foi concedido
        assertEquals(List.of("CLIENTE"), perfis);

        // consulta a situação do usuário no banco de dados
        Boolean ativo = jdbcTemplate.queryForObject("""
            SELECT ativo
            FROM usuario
            WHERE email = ?
            """, Boolean.class, email);

        // garante que a situação do usuário foi definida pelo servidor
        assertEquals(Boolean.TRUE, ativo);
    }

    // verifica a normalização dos dados e a prevenção de e-mails duplicados
    @Test
    void deveNormalizarDadosEImpedirEmailDuplicado() throws Exception {

        // gera um e-mail exclusivo em letras minúsculas
        String email = gerarEmailTeste();

        // converte o e-mail para letras maiúsculas
        String emailMaiusculo = email.toUpperCase(Locale.ROOT);

        // monta os dados com espaços no nome e e-mail em maiúsculas
        String corpoRequisicao = criarJsonCadastro(
                "   Cliente Teste Normalizacao   ",
                emailMaiusculo,
                SENHA_TESTE
        );

        // realiza o cadastro e verifica os dados normalizados
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRequisicao))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Cliente Teste Normalizacao"))
                .andExpect(jsonPath("$.email").value(email));

        // consulta o e-mail efetivamente armazenado no banco
        String emailPersistido = jdbcTemplate.queryForObject("""
            SELECT email
            FROM usuario
            WHERE email = ?
            """, String.class, email);

        // garante que o e-mail foi persistido em letras minúsculas
        assertEquals(email, emailPersistido);

        // monta uma nova requisição utilizando o e-mail em minúsculas
        String corpoDuplicado = criarJsonCadastro(
                "Outro Cliente",
                email,
                SENHA_TESTE
        );

        // verifica se a API impede o cadastro duplicado
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDuplicado))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem")
                        .value("Já existe um usuário cadastrado com este e-mail."));

        // consulta a quantidade de usuários associados ao e-mail
        Integer quantidade = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM usuario
            WHERE email = ?
            """, Integer.class, email);

        // garante que apenas um usuário foi persistido
        assertEquals(1, quantidade);
    }

    // verifica se um cliente recém-cadastrado consegue autenticar-se, mas não possui acesso aos recursos administrativos.
    @Test
    void deveAutenticarClienteCadastradoEImpedirAcessoAdministrativo()
            throws Exception {

        // gera um e-mail exclusivo para o teste
        String email = gerarEmailTeste();

        // monta os dados do novo cliente
        String corpoCadastro = criarJsonCadastro(
                "Cliente Teste Autenticacao",
                email,
                SENHA_TESTE
        );

        // realiza o cadastro do cliente
        mockMvc.perform(post("/api/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCadastro))
                .andExpect(status().isCreated());

        // monta as credenciais utilizadas na autenticação
        String corpoLogin = """
            {
                "email": "%s",
                "senha": "%s"
            }
            """.formatted(email, SENHA_TESTE);

        // realiza o login e captura o corpo da resposta
        String corpoResposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").value(3600))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // extrai o token JWT sem exibi-lo no terminal
        String token = JsonPath.read(corpoResposta, "$.token");

        // tenta acessar um recurso exclusivamente administrativo
        mockMvc.perform(get("/api/movimentacoes-estoque")
                        .header("Authorization", "Bearer " + token))

                // Garante que o cliente autenticado não possui essa permissão.
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.erro").value("Acesso negado"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Você não possui permissão para acessar este recurso."));
    }
}