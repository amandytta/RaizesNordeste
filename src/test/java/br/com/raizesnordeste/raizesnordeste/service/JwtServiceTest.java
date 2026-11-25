package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.LoginResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;

import java.time.Instant;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;


// habilita a integracao entre o JUnit 5 e o Mockito
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    // define o emissor utilizado exclusivamente nos testes
    private static final String EMISSOR = "raizes-nordeste-test";

    // define o tempo de validade do token em segundos
    private static final long EXPIRACAO = 3600L;

    // simula o componente responsavel pela codificacao do JWT
    @Mock
    private JwtEncoder jwtEncoder;

    // representa o serviço que será testado
    private JwtService jwtService;

    // prepara uma nova instância do serviço antes de cada teste
    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                jwtEncoder,
                EMISSOR,
                EXPIRACAO
        );
    }

    // verifica se o servico gera e retorna corretamente um token JWT
    @Test
    void deveGerarTokenJwtComSucesso() {

        // cria uma autenticacao ficticia com o perfil ADMIN
        var autenticacao = new UsernamePasswordAuthenticationToken(
                "admin@raizes.local",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // cria um token ficticio para simular o retorno do codificador
        Jwt jwt = Jwt.withTokenValue("token-jwt-de-teste")
                .header("alg", "HS256")
                .claim("sub", "admin@raizes.local")
                .build();

        // configura o comportamento esperado do codificador JWT
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        // executa o metodo responsavel pela geracao do token
        LoginResponseDTO resposta = jwtService.gerarToken(autenticacao);

        // verifica se a resposta foi criada corretamente
        assertNotNull(resposta);

        // verifica se o token retornado corresponde ao token gerado
        assertEquals("token-jwt-de-teste", resposta.token());

        // verifica se o tipo de autenticacao e Bearer
        assertEquals("Bearer", resposta.tipo());

        // verifica se o tempo de validade está correto
        assertEquals(EXPIRACAO, resposta.expiraEm());

        // confirma que o codificador JWT foi chamado
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    // verifica se o subject do JWT corresponde ao usuario autenticado
    @Test
    void deveIncluirEmailDoUsuarioNoSubjectDoToken() {

        // define o email ficticio do usuario autenticado
        String email = "cliente@raizes.local";

        // cria uma autenticacao ficticia com o perfil CLIENTE
        var autenticacao = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );

        // cria um token ficticio para simular o retorno do codificador
        Jwt jwt = Jwt.withTokenValue("token-jwt-de-teste")
                .header("alg", "HS256")
                .claim("sub", email)
                .build();

        // configura o comportamento do codificador JWT
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        // executa a geracao do token
        jwtService.gerarToken(autenticacao);

        // cria um capturador dos parametros enviados ao codificador
        ArgumentCaptor<JwtEncoderParameters> capturador =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        // captura os parametros utilizados na geracao do JWT
        verify(jwtEncoder).encode(capturador.capture());

        // recupera o subject enviado pelo serviço
        String subject = capturador.getValue()
                .getClaims()
                .getSubject();

        // confirma que o subject corresponde ao e-mail autenticado
        assertEquals(email, subject);
    }

    // verifica se o JWT utiliza o emissor configurado na aplicacao
    @Test
    void deveIncluirEmissorConfiguradoNoToken() {

        // cria uma autenticacao ficticia com o perfil ADMIN
        var autenticacao = new UsernamePasswordAuthenticationToken(
                "admin@raizes.local",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // cria um token ficticio para simular o retorno do codificador
        Jwt jwt = Jwt.withTokenValue("token-jwt-de-teste")
                .header("alg", "HS256")
                .claim("sub", "admin@raizes.local")
                .build();

        // configura o comportamento esperado do codificador JWT
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        // executa a geracao do token
        jwtService.gerarToken(autenticacao);

        // cria um capturador dos parametros enviados ao codificador
        ArgumentCaptor<JwtEncoderParameters> capturador =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        // captura os parametros utilizados na geracao do JWT
        verify(jwtEncoder).encode(capturador.capture());

        // recupera a claim que identifica o emissor do token
        String emissor = capturador.getValue()
                .getClaims()
                .getClaimAsString("iss");

        // confirma que o emissor corresponde ao valor configurado
        assertEquals(EMISSOR, emissor);
    }

    // verifica os horarios de emissao e expiracao do token JWT.
    @Test
    void deveDefinirCorretamenteEmissaoEExpiracaoDoToken() {

        // cria uma autenticacao ficticia com o perfil ADMIN
        var autenticacao = new UsernamePasswordAuthenticationToken(
                "admin@raizes.local",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // cria um token ficticio para simular o retorno do codificador
        Jwt jwt = Jwt.withTokenValue("token-jwt-de-teste")
                .header("alg", "HS256")
                .claim("sub", "admin@raizes.local")
                .build();

        // configura o comportamento esperado do codificador JWT
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        // registra o instante anterior a geracao do token
        Instant antes = Instant.now();

        // executa o metodo responsavel pela geracao do token
        jwtService.gerarToken(autenticacao);

        // registra o instante posterior a geracao do token
        Instant depois = Instant.now();

        // cria um capturador dos parametros enviados ao codificador
        ArgumentCaptor<JwtEncoderParameters> capturador =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        // captura os parametros utilizados na geracao do JWT
        verify(jwtEncoder).encode(capturador.capture());

        // recupera os horarios de emissao e expiracao
        Instant emissao = capturador.getValue()
                .getClaims()
                .getIssuedAt();

        Instant expiracao = capturador.getValue()
                .getClaims()
                .getExpiresAt();

        // verifica se os dois horarios foram definidos
        assertNotNull(emissao);
        assertNotNull(expiracao);

        // confirma que a emissao ocorreu durante a execucao do metodo
        assertTrue(!emissao.isBefore(antes));
        assertTrue(!emissao.isAfter(depois));

        // confirma que a expiracao ocorre depois da emissao
        assertTrue(expiracao.isAfter(emissao));

        // verifica se a duracao corresponde exatamente a validade configurada
        assertEquals(
                Duration.ofSeconds(EXPIRACAO),
                Duration.between(emissao, expiracao)
        );
    }

    // verifica se o perfil do usuario e incluido corretamente no JWT
    @Test
    void deveIncluirPerfilDoUsuarioNasClaimsDoToken() {

        // cria uma autenticacao ficticia com o perfil GERENTE
        var autenticacao = new UsernamePasswordAuthenticationToken(
                "gerente@raizes.local",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))
        );

        // cria um token ficticio para simular o retorno do codificador
        Jwt jwt = Jwt.withTokenValue("token-jwt-de-teste")
                .header("alg", "HS256")
                .claim("sub", "gerente@raizes.local")
                .build();

        // configura o comportamento esperado do codificador JWT
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        // executa a geracao do token
        jwtService.gerarToken(autenticacao);

        // cria um capturador dos parametros enviados ao codificador
        ArgumentCaptor<JwtEncoderParameters> capturador =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        // captura os parametros utilizados na geracao do JWT
        verify(jwtEncoder).encode(capturador.capture());

        // recupera a lista de perfis incluida nas claims do token
        List<String> perfis = capturador.getValue()
                .getClaims()
                .getClaimAsStringList("roles");

        // verifica se a lista de perfis foi definida
        assertNotNull(perfis);

        // confirma que somente o perfil esperado foi incluido
        assertEquals(List.of("ROLE_GERENTE"), perfis);
    }

    // verifica se o servico utiliza o algoritmo HS256 na geracao do JWT
    @Test
    void deveUtilizarAlgoritmoHS256NaGeracaoDoToken() {

        // cria uma autenticacao ficticia com o perfil ATENDENTE
        var autenticacao = new UsernamePasswordAuthenticationToken(
                "atendente@raizes.local",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ATENDENTE"))
        );

        // cria um token ficticio para simular o retorno do codificador
        Jwt jwt = Jwt.withTokenValue("token-jwt-de-teste")
                .header("alg", "HS256")
                .claim("sub", "atendente@raizes.local")
                .build();

        // configura o comportamento esperado do codificador JWT
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        // executa o metodo responsavel pela geracao do token
        jwtService.gerarToken(autenticacao);

        // cria um capturador dos parametros enviados ao codificador
        ArgumentCaptor<JwtEncoderParameters> capturador =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        // captura os parametros utilizados na geracao do JWT
        verify(jwtEncoder).encode(capturador.capture());

        // recupera o cabecalho enviado pelo servico
        var cabecalho = capturador.getValue().getJwsHeader();

        // verifica se o cabecalho foi definido
        assertNotNull(cabecalho);

        // confirma que o algoritmo utilizado corresponde ao HS256
        assertEquals(
                MacAlgorithm.HS256.getName(),
                cabecalho.getAlgorithm().getName()
        );
    }

    // verifica se todos os perfis do usuario são incluidos no JWT
    @Test
    void deveIncluirMultiplosPerfisNasClaimsDoToken() {

        // cria uma autenticacao ficticia com dois perfis de acesso
        var autenticacao = new UsernamePasswordAuthenticationToken(
                "usuario.multiperfil@raizes.local",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_GERENTE"),
                        new SimpleGrantedAuthority("ROLE_ATENDENTE")
                )
        );

        // cria um token ficticio para simular o retorno do codificador
        Jwt jwt = Jwt.withTokenValue("token-jwt-de-teste")
                .header("alg", "HS256")
                .claim("sub", "usuario.multiperfil@raizes.local")
                .build();

        // configura o comportamento esperado do codificador JWT
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        // executa o metodo responsavel pela geracao do token
        jwtService.gerarToken(autenticacao);

        // cria um capturador dos parametros enviados ao codificador
        ArgumentCaptor<JwtEncoderParameters> capturador =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        // captura os parametros utilizados na geracao do JWT
        verify(jwtEncoder).encode(capturador.capture());

        // recupera a lista de perfis incluida nas claims
        List<String> perfis = capturador.getValue()
                .getClaims()
                .getClaimAsStringList("roles");

        // verifica se a lista de perfis foi definida
        assertNotNull(perfis);

        // confirma que exatamente dois perfis foram incluidos
        assertEquals(2, perfis.size());

        // verifica os perfis independentemente da ordem
        assertEquals(
                Set.of("ROLE_GERENTE", "ROLE_ATENDENTE"),
                Set.copyOf(perfis)
        );
    }

    // garante que fatores de autenticacao nao sejam incluidos na claim destinada exclusivamente aos perfis de acesso.
    @Test
    void deveExcluirFatoresDeAutenticacaoDaClaimRoles() {

        // simula um administrador autenticado por senha
        Authentication autenticacao =
                new UsernamePasswordAuthenticationToken(
                        "admin@raizes.local",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("FACTOR_PASSWORD")
                        )
                );

        // simula o retorno do codificador JWT
        Jwt jwt = mock(Jwt.class);

        when(jwt.getTokenValue()).thenReturn("token-jwt-teste");

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        // executa a geracao do token
        jwtService.gerarToken(autenticacao);

        // captura os parametros enviados ao codificador
        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(captor.capture());

        // recupera os perfis incluidos na claim roles
        List<String> perfis = captor.getValue()
                .getClaims()
                .getClaimAsStringList("roles");

        // garante que somente o perfil de acesso foi incluido
        assertEquals(List.of("ROLE_ADMIN"), perfis);
    }
}