package br.com.raizesnordeste.raizesnordeste.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.JsonPath;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


// carrega o contexto completo da aplicação Spring Boot
@SpringBootTest

// configura o MockMvc para testar os endpoints da API
@AutoConfigureMockMvc

// ativa exclusivamente o perfil de testes
@ActiveProfiles("test")
class AuthIntegrationTest {

    // permite realizar requisições HTTP simuladas aos endpoints
    @Autowired
    private MockMvc mockMvc;

    // permite consultar e preparar os dados no banco de testes
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // utiliza o codificador real de senhas da aplicação
    @Autowired
    private PasswordEncoder passwordEncoder;

    // utiliza o decodificador real para validar os tokens JWT
    @Autowired
    private JwtDecoder jwtDecoder;

    // injeta o componente responsável por assinar tokens JWT
    @Autowired
    private JwtEncoder jwtEncoder;

    // define uma senha ficticia exclusiva para os testes
    private static final String SENHA_TESTE = "SenhaIntegracao@2026!";

    // armazena os e-mails dos usuarios criados durante cada teste
    private final List<String> emailsUsuariosTeste = new ArrayList<>();

    // verifica se a aplicação esta conectada ao ambiente de testes
    @BeforeEach
    void verificarAmbienteDeTestes() {

        // consulta o nome do banco de dados utilizado pela conexão
        String bancoAtual = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        // garante que o banco utilizado seja exclusivamente o de testes
        assertEquals(
                "raizes_nordeste_test",
                bancoAtual,
                "Operacao interrompida: banco de dados incorreto."
        );

        // consulta o usuario MySQL utilizado pela conexao
        String usuarioAtual = jdbcTemplate.queryForObject(
                "SELECT CURRENT_USER()",
                String.class
        );

        // garante que a conexao utilize o usuario exclusivo de testes
        assertEquals(
                "raizes_test@localhost",
                usuarioAtual,
                "Operacao interrompida: usuario MySQL incorreto."
        );
    }

    // cria um usuario ficticio com o perfil e a situacao informados
    private String criarUsuarioTeste(String perfil, boolean ativo) {

        // confirma que estamos utilizando o banco exclusivo de testes
        verificarAmbienteDeTestes();

        // gera um e-mail unico para evitar conflitos entre os testes
        String email = "jwt.teste." + UUID.randomUUID()
                + "@raizes.local";

        // gera o hash da senha utilizando o BCrypt real
        String senhaHash = passwordEncoder.encode(SENHA_TESTE);

        // registra o e-mail para permitir a limpeza posterior
        emailsUsuariosTeste.add(email);

        // insere o usuario ficticio no banco de testes
        jdbcTemplate.update(
                """
                INSERT INTO usuario (nome, email, senha_hash, ativo)
                VALUES (?, ?, ?, ?)
                """,
                "Usuario Teste JWT",
                email,
                senhaHash,
                ativo
        );

        // recupera o identificador do usuario criado
        Long usuarioId = jdbcTemplate.queryForObject(
                "SELECT id FROM usuario WHERE email = ?",
                Long.class,
                email
        );

        // recupera o identificador do perfil solicitado
        Long perfilId = jdbcTemplate.queryForObject(
                "SELECT id FROM perfil WHERE nome = ?",
                Long.class,
                perfil
        );

        // associa o usuario ficticio ao perfil de acesso
        jdbcTemplate.update(
                """
                INSERT INTO usuario_perfil (usuario_id, perfil_id)
                VALUES (?, ?)
                """,
                usuarioId,
                perfilId
        );

        // retorna o e-mail para utilizacao nas requisicoes de login
        return email;
    }

    // remove os usuarios ficticios criados durante cada teste
    @AfterEach
    void limparDadosDeTeste() {

        // confirma que estamos utilizando o banco exclusivo de testes
        verificarAmbienteDeTestes();

        // percorre os e-mails registrados durante o teste
        for (String email : emailsUsuariosTeste) {

            // remove primeiro as associacoes entre usuario e perfil
            jdbcTemplate.update(
                    """
                    DELETE up
                    FROM usuario_perfil up
                    INNER JOIN usuario u ON u.id = up.usuario_id
                    WHERE u.email = ?
                    """,
                    email
            );

            // remove o usuario ficticio correspondente ao e-mail
            jdbcTemplate.update(
                    "DELETE FROM usuario WHERE email = ?",
                    email
            );
        }

        // limpa a lista auxiliar de e-mails
        emailsUsuariosTeste.clear();
    }

    // verifica se um usuario ativo consegue realizar login e receber um token JWT utilizando a autenticacao real.
    @Test
    void deveRealizarLoginComSucesso() throws Exception {

        // cria um usuario administrador ativo no banco de testes
        String email = criarUsuarioTeste("ADMIN", true);

        // monta o corpo JSON da requisicao de login
        String corpoRequisicao = """
                {
                    "email": "%s",
                    "senha": "%s"
                }
                """.formatted(email, SENHA_TESTE);

        // envia uma requisicao ao endpoint real de autenticacao
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoRequisicao)
                )

                // verifica se a requisicao retorna HTTP 200
                .andExpect(status().isOk())

                // verifica se um token foi retornado
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())

                // confirma que o tipo do token corresponde a Bearer
                .andExpect(jsonPath("$.tipo").value("Bearer"))

                // confirma o tempo de validade do token em segundos
                .andExpect(jsonPath("$.expiraEm").value(3600))

                // garante que a resposta de login nao seja armazenada em cache
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    // verifica a assinatura e as claims do JWT retornado pelo login
    @Test
    void deveGerarTokenJwtValidoComClaimsCorretas() throws Exception {

        // cria um usuario administrador ativo no banco de testes
        String email = criarUsuarioTeste("ADMIN", true);

        // monta o corpo JSON da requisicao de login
        String corpoRequisicao = """
                {
                    "email": "%s",
                    "senha": "%s"
                }
                """.formatted(email, SENHA_TESTE);

        // realiza o login e recupera o corpo da resposta
        String corpoResposta = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoRequisicao)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // extrai o token JWT retornado pela API
        String token = JsonPath.read(corpoResposta, "$.token");

        // valida a assinatura e as claims utilizando o decodificador real
        Jwt jwt = jwtDecoder.decode(token);

        // confirma que o token foi decodificado com sucesso
        assertNotNull(jwt);

        // verifica a identidade do usuario autenticado
        assertEquals(email, jwt.getSubject());

        // confirma o emissor configurado para o ambiente de testes
        assertEquals(
                "raizes-nordeste-test",
                jwt.getClaimAsString("iss")
        );

        // verifica o algoritmo de assinatura utilizado
        assertEquals("HS256", jwt.getHeaders().get("alg"));

        // confirma o perfil de acesso presente no token
        assertEquals(
                List.of("ROLE_ADMIN"),
                jwt.getClaimAsStringList("roles")
        );

        // verifica se os horarios de emissao e expiracao existem
        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());

        // confirma que o token possui validade de uma hora
        assertEquals(
                Duration.ofSeconds(3600),
                Duration.between(
                        jwt.getIssuedAt(),
                        jwt.getExpiresAt()
                )
        );
    }

    // verifica se um administrador autenticado com JWT valido consegue acessar um endpoint protegido da API
    @Test
    void devePermitirAcessoAdministrativoComTokenValido() throws Exception {

        // cria um usuario administrador ativo no banco de testes
        String email = criarUsuarioTeste("ADMIN", true);

        // monta o corpo JSON da requisicao de login
        String corpoRequisicao = """
                {
                    "email": "%s",
                    "senha": "%s"
                }
                """.formatted(email, SENHA_TESTE);

        // realiza o login utilizando a autenticacao real
        String corpoResposta = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoRequisicao)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // extrai o token JWT retornado pela API
        String token = JsonPath.read(corpoResposta, "$.token");

        // acessa um endpoint protegido utilizando o token Bearer
        mockMvc.perform(
                        get("/api/movimentacoes-estoque")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )

                // confirma que o administrador possui permissao
                .andExpect(status().isOk())

                // confirma que a resposta possui uma lista de movimentacoes
                .andExpect(jsonPath("$").isArray());
    }

    // verifica se um usuário não autenticado consegue consultar os produtos disponibilizados publicamente.
    @Test
    void devePermitirAcessoPublicoSemToken() throws Exception {

        // realiza uma requisicao sem o cabecalho Authorization
        mockMvc.perform(
                        get("/api/produtos")
                )

                // confirma que o acesso publico foi autorizado
                .andExpect(status().isOk())

                // confirma que a resposta possui uma lista de produtos
                .andExpect(jsonPath("$").isArray());
    }

    // verifica se um usuario sem token JWT e impedido de acessar um endpoint protegido da API.
    @Test
    void deveBloquearAcessoProtegidoSemToken() throws Exception {

        // realiza uma requisicao sem o cabecalho Authorization
        mockMvc.perform(
                        get("/api/movimentacoes-estoque")
                )

                // confirma que o acesso nao autenticado foi bloqueado
                .andExpect(status().isUnauthorized())

                // verifica o codigo de status no corpo da resposta
                .andExpect(jsonPath("$.status").value(401))

                // confirma o título padronizado do erro
                .andExpect(jsonPath("$.erro")
                        .value("Não autorizado"))

                // verifica a mensagem de erro retornada pela API
                .andExpect(jsonPath("$.mensagem")
                        .value("Token ausente, inválido ou expirado."))

                // confirma o caminho da requisicao bloqueada
                .andExpect(jsonPath("$.caminho")
                        .value("/api/movimentacoes-estoque"))

                // verifica se o timestamp esta presente
                .andExpect(jsonPath("$.timestamp").isNotEmpty())

                // confirma o esquema de autenticacao exigido
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer"
                ));
    }

    // verifica se a API bloqueia requisicoes que apresentam um token JWT invalido
    @Test
    void deveBloquearAcessoComTokenInvalido() throws Exception {

        // define um token ficticio com formato invalido
        String tokenInvalido = "token-invalido-de-teste";

        // realiza uma requisicao ao endpoint protegido
        mockMvc.perform(
                        get("/api/movimentacoes-estoque")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenInvalido
                                )
                )

                // confirma que o acesso foi bloqueado
                .andExpect(status().isUnauthorized())

                // verifica o status no corpo da resposta
                .andExpect(jsonPath("$.status").value(401))

                // confirma o título padronizado do erro
                .andExpect(jsonPath("$.erro")
                        .value("Não autorizado"))

                // verifica a mensagem de erro retornada
                .andExpect(jsonPath("$.mensagem")
                        .value("Token ausente, inválido ou expirado."))

                // confirma o caminho da requisição
                .andExpect(jsonPath("$.caminho")
                        .value("/api/movimentacoes-estoque"))

                // verifica a presença do timestamp
                .andExpect(jsonPath("$.timestamp").isNotEmpty())

                // confirma o esquema de autenticacao exigido
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer"
                ));
    }

    // verifica se um cliente autenticado com JWT valido e impedido de acessar um endpoint administrativo
    @Test
    void deveBloquearAcessoAdministrativoComTokenDeCliente() throws Exception {

        // cria um usuario ativo com perfil CLIENTE
        String email = criarUsuarioTeste("CLIENTE", true);

        // monta o corpo JSON da requisicao de login
        String corpoRequisicao = """
                {
                    "email": "%s",
                    "senha": "%s"
                }
                """.formatted(email, SENHA_TESTE);

        // realiza o login utilizando a autenticacao real
        String corpoResposta = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoRequisicao)
                )

                // confirma que o cliente conseguiu se autenticar
                .andExpect(status().isOk())

                // recupera o corpo da resposta
                .andReturn()
                .getResponse()
                .getContentAsString();

        // extrai o token JWT retornado pela API
        String token = JsonPath.read(corpoResposta, "$.token");

        // tenta acessar um endpoint exclusivo de administradores
        mockMvc.perform(
                        get("/api/movimentacoes-estoque")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )

                // confirma que o cliente não possui permissao
                .andExpect(status().isForbidden())

                // verifica o status no corpo da resposta
                .andExpect(jsonPath("$.status").value(403))

                // confirma o título padronizado do erro
                .andExpect(jsonPath("$.erro")
                        .value("Acesso negado"))

                // verifica a mensagem de acesso negado
                .andExpect(jsonPath("$.mensagem")
                        .value("Você não possui permissão para acessar este recurso."))

                // confirma o caminho da requisição bloqueada
                .andExpect(jsonPath("$.caminho")
                        .value("/api/movimentacoes-estoque"))

                // verifica a presença do timestamp
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // verifica se a API rejeita a autenticação de um usuario existente que informa uma senha incorreta
    @Test
    void deveRejeitarLoginComSenhaIncorreta() throws Exception {

        // cria um usuario administrador ativo no banco de testes
        String email = criarUsuarioTeste("ADMIN", true);

        // monta a requisição utilizando uma senha incorreta
        String corpoRequisicao = """
                {
                    "email": "%s",
                    "senha": "SenhaIncorreta@2026!"
                }
                """.formatted(email);

        // realiza uma tentativa de login com credenciais invalidas
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoRequisicao)
                )

                // confirma que a autenticacao foi rejeitada
                .andExpect(status().isUnauthorized())

                // verifica o status no corpo da resposta
                .andExpect(jsonPath("$.status").value(401))

                // confirma o titulo padronizado do erro
                .andExpect(jsonPath("$.erro")
                        .value("Não autorizado"))

                // verifica a mensagem generica de autenticação
                .andExpect(jsonPath("$.mensagem")
                        .value("Credenciais inválidas."))

                // confirma o caminho da requisição
                .andExpect(jsonPath("$.caminho")
                        .value("/api/auth/login"))

                // verifica a presenca do timestamp
                .andExpect(jsonPath("$.timestamp").isNotEmpty())

                // garante que nenhum token JWT foi retornado
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    // verifica se a API rejeita tentativas de autenticação realizadas com um usuario inexistente
    @Test
    void deveRejeitarLoginComUsuarioInexistente() throws Exception {

        // gera um endereco de e-mail exclusivo para o teste
        String email = "inexistente." + UUID.randomUUID()
                + "@raizes.local";

        // confirma que o usuario nao existe no banco de testes
        Integer quantidade = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE email = ?",
                Integer.class,
                email
        );

        assertEquals(0, quantidade);

        // monta o corpo JSON da requisição de login
        String corpoRequisicao = """
                {
                    "email": "%s",
                    "senha": "%s"
                }
                """.formatted(email, SENHA_TESTE);

        // realiza uma tentativa de login com usuario inexistente
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoRequisicao)
                )

                // confirma que a autenticação foi rejeitada
                .andExpect(status().isUnauthorized())

                // verifica o status no corpo da resposta
                .andExpect(jsonPath("$.status").value(401))

                // confirma o título padronizado do erro
                .andExpect(jsonPath("$.erro")
                        .value("Não autorizado"))

                // verifica a mensagem generica de autenticação
                .andExpect(jsonPath("$.mensagem")
                        .value("Credenciais inválidas."))

                // confirma o caminho da requisição
                .andExpect(jsonPath("$.caminho")
                        .value("/api/auth/login"))

                // verifica a presença do timestamp
                .andExpect(jsonPath("$.timestamp").isNotEmpty())

                // confirma que nenhum token JWT foi retornado
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    // verifica se a API impede a autenticação de um usuario inativo, mesmo com credenciais corretas
    @Test
    void deveRejeitarLoginComUsuarioInativo() throws Exception {

        // cria um usuario administrador inativo no banco de testes
        String email = criarUsuarioTeste("ADMIN", false);

        // monta a requisição utilizando as credenciais corretas
        String corpoRequisicao = """
                {
                    "email": "%s",
                    "senha": "%s"
                }
                """.formatted(email, SENHA_TESTE);

        // realiza uma tentativa de login com a conta inativa
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoRequisicao)
                )

                // confirma que a autenticacao foi rejeitada
                .andExpect(status().isUnauthorized())

                // verifica o status no corpo da resposta
                .andExpect(jsonPath("$.status").value(401))

                // confirma o título padronizado do erro
                .andExpect(jsonPath("$.erro")
                        .value("Não autorizado"))

                // verifica a mensagem genérica de autenticação
                .andExpect(jsonPath("$.mensagem")
                        .value("Credenciais inválidas."))

                // confirma o caminho da requisição
                .andExpect(jsonPath("$.caminho")
                        .value("/api/auth/login"))

                // verifica a presença do timestamp
                .andExpect(jsonPath("$.timestamp").isNotEmpty())

                // confirma que nenhum token JWT foi retornado
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    // verifica se a API rejeita requisiçoes de login que possuem dados fora das regras de validação
    @Test
    void deveRejeitarLoginComDadosInvalidos() throws Exception {

        // monta uma requisição com e-mail invalido e senha vazia
        String corpoRequisicao = """
                {
                    "email": "email-invalido",
                    "senha": ""
                }
                """;

        // realiza uma tentativa de login com dados invalidos
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoRequisicao)
                )

                // confirma que a requisição foi rejeitada
                .andExpect(status().isBadRequest())

                // verifica o status no corpo da resposta
                .andExpect(jsonPath("$.status").value(400))

                // confirma o título padronizado do erro
                .andExpect(jsonPath("$.erro")
                        .value("Bad Request"))

                // verifica a mensagem de validação do e-mail
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString(
                                "O e-mail informado é inválido."
                        )
                ))

                // verifica a mensagem de validação da senha
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString(
                                "A senha é obrigatória."
                        )
                ))

                // confirma o caminho da requisição
                .andExpect(jsonPath("$.caminho")
                        .value("/api/auth/login"))

                // verifica a presença do timestamp
                .andExpect(jsonPath("$.timestamp").isNotEmpty())

                // confirma que nenhum token JWT foi retornado
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    // verifica se a API rejeita um token JWT que teve sua assinatura digital adulterada
    @Test
    void deveRejeitarTokenComAssinaturaAdulterada() throws Exception {

        // cria um usuario administrador ativo no banco de testes
        String email = criarUsuarioTeste("ADMIN", true);

        // monta o corpo JSON da requisicao de login
        String corpoRequisicao = """
                {
                    "email": "%s",
                    "senha": "%s"
                }
                """.formatted(email, SENHA_TESTE);

        // realiza o login utilizando a autenticacao real
        String corpoResposta = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoRequisicao)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // extrai o token JWT original
        String tokenOriginal = JsonPath.read(
                corpoResposta, "$.token"
        );

        // confirma que o token original permite o acesso
        mockMvc.perform(
                        get("/api/movimentacoes-estoque")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenOriginal
                                )
                )
                .andExpect(status().isOk());

        // separa o JWT em cabeçalho, payload e assinatura
        String[] partes = tokenOriginal.split("\\.");

        // confirma que o token possui as tres partes esperadas
        assertEquals(3, partes.length);

        // recupera a assinatura original
        String assinaturaOriginal = partes[2];

        // escolhe um caractere diferente do primeiro caractere da assinatura original.
        char novoCaractere =
                assinaturaOriginal.charAt(0) == 'A' ? 'B' : 'A';

        // altera somente o primeiro caractere da assinatura
        String assinaturaAdulterada =
                novoCaractere + assinaturaOriginal.substring(1);

        // reconstrói o JWT mantendo o cabeçalho e o payload
        String tokenAdulterado = partes[0] + "."
                + partes[1] + "." + assinaturaAdulterada;

        // tenta acessar o endpoint com o token adulterado
        mockMvc.perform(
                        get("/api/movimentacoes-estoque")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenAdulterado
                                )
                )

                // confirma que o token adulterado foi rejeitado
                .andExpect(status().isUnauthorized())

                // verifica o status no corpo da resposta
                .andExpect(jsonPath("$.status").value(401))

                // confirma o título padronizado do erro
                .andExpect(jsonPath("$.erro")
                        .value("Não autorizado"))

                // verifica a mensagem de erro retornada
                .andExpect(jsonPath("$.mensagem")
                        .value("Token ausente, inválido ou expirado."))

                // confirma o caminho da requisição
                .andExpect(jsonPath("$.caminho")
                        .value("/api/movimentacoes-estoque"))

                // verifica a presenca do timestamp
                .andExpect(jsonPath("$.timestamp").isNotEmpty())

                // confirma o esquema de autenticação exigido
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer"
                ));
    }

    // verifica se a API rejeita um token JWT expirado, mesmo quando sua assinatura criptográfica e valida
    @Test
    void deveRejeitarTokenExpirado() throws Exception {

        // cria um usuario administrador ativo no banco de testes
        String email = criarUsuarioTeste("ADMIN", true);

        // recupera o instante atual
        Instant agora = Instant.now();

        // define a emissao do token duas horas atrás
        Instant emissao = agora.minusSeconds(7200);

        // define a expiracao do token uma hora atrás
        Instant expiracao = agora.minusSeconds(3600);

        // monta as claims de um token já expirado
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("raizes-nordeste-test")
                .subject(email)
                .issuedAt(emissao)
                .expiresAt(expiracao)
                .claim("roles", List.of("ROLE_ADMIN"))
                .build();

        // define o algoritmo utilizado na assinatura
        JwsHeader cabecalho = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        // gera um token expirado com assinatura valida
        String tokenExpirado = jwtEncoder.encode(
                JwtEncoderParameters.from(cabecalho, claims)
        ).getTokenValue();

        // confirma que a data de expiracao esta no passado
        assertEquals(true, expiracao.isBefore(agora));

        // tenta acessar um endpoint protegido com o token expirado
        mockMvc.perform(
                        get("/api/movimentacoes-estoque")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenExpirado
                                )
                )

                // confirma que o acesso foi bloqueado
                .andExpect(status().isUnauthorized())

                // verifica o status no corpo da resposta
                .andExpect(jsonPath("$.status").value(401))

                // confirma o título padronizado do erro
                .andExpect(jsonPath("$.erro")
                        .value("Não autorizado"))

                // verifica a mensagem de erro retornada
                .andExpect(jsonPath("$.mensagem")
                        .value("Token ausente, inválido ou expirado."))

                // confirma o caminho da requisição
                .andExpect(jsonPath("$.caminho")
                        .value("/api/movimentacoes-estoque"))

                // verifica a presença do timestamp
                .andExpect(jsonPath("$.timestamp").isNotEmpty())

                // confirma o esquema de autenticação exigido
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer"
                ));
    }
}