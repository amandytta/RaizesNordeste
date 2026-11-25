package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.LoginRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.LoginResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;


// habilita a integracao do Mockito com o JUnit 5
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // simula o componente responsavel pela autenticacao dos usuarios
    @Mock
    private AuthenticationManager authenticationManager;

    // simula o serviço responsavel pela geracao dos tokens JWT
    @Mock
    private JwtService jwtService;

    // instancia real do serviço que será testado
    private AuthService authService;

    // prepara uma nova instância do serviço antes de cada teste
    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                jwtService
        );
    }

    // verifica se o serviço autentica o usuario e retorna o JWT com sucesso
    @Test
    void deveAutenticarUsuarioComSucesso() {

        // cria um DTO com credenciais ficticias para o teste
        LoginRequestDTO dto = new LoginRequestDTO(
                "admin@raizes.local",
                "SenhaFicticiaDeTeste123!"
        );

        // simula uma autenticacao realizada com sucesso
        var autenticacao = new UsernamePasswordAuthenticationToken(
                dto.email(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // cria a resposta esperada da geracao do token JWT
        LoginResponseDTO respostaEsperada = new LoginResponseDTO(
                "token-jwt-de-teste",
                "Bearer",
                3600L
        );

        // simula a validacao bem-sucedida das credenciais
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(autenticacao);

        // simula a geracao do JWT para o usuario autenticado
        when(jwtService.gerarToken(autenticacao))
                .thenReturn(respostaEsperada);

        // executa o metodo responsavel pela autenticacao
        LoginResponseDTO resposta = authService.autenticar(dto);

        // verifica se a resposta foi retornada
        assertNotNull(resposta);

        // confirma se o token retornado corresponde ao esperado
        assertEquals("token-jwt-de-teste", resposta.token());

        // verifica se o tipo do token corresponde ao padrao Bearer
        assertEquals("Bearer", resposta.tipo());

        // confirma o tempo de validade informado na resposta
        assertEquals(3600L, resposta.expiraEm());
    }

    // verifica se as credenciais sao encaminhadas corretamente ao componente responsavel pela autenticacao.
    @Test
    void deveEncaminharCredenciaisAoAuthenticationManager() {

        // cria um DTO com credenciais ficticias
        LoginRequestDTO dto = new LoginRequestDTO(
                "cliente@raizes.local",
                "SenhaFicticiaCliente123!"
        );

        // simula uma autenticacao realizada com sucesso
        var autenticacao = new UsernamePasswordAuthenticationToken(
                dto.email(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );

        // configura o retorno do componente de autenticacao
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(autenticacao);

        // simula a resposta da geracao do token JWT
        when(jwtService.gerarToken(autenticacao))
                .thenReturn(new LoginResponseDTO(
                        "token-jwt-de-teste",
                        "Bearer",
                        3600L
                ));

        // executa o metodo responsavel pela autenticacao
        authService.autenticar(dto);

        // cria um capturador para recuperar a autenticacao enviada
        ArgumentCaptor<Authentication> capturador =
                ArgumentCaptor.forClass(Authentication.class);

        // captura o argumento enviado ao AuthenticationManager
        verify(authenticationManager).authenticate(capturador.capture());

        // recupera a solicitacao de autenticacao capturada
        Authentication solicitacao = capturador.getValue();

        // confirma que o e-mail foi encaminhado corretamente
        assertEquals(dto.email(), solicitacao.getPrincipal());

        // confirma que a senha foi encaminhada corretamente
        assertEquals(dto.senha(), solicitacao.getCredentials());

        // garante que a solicitacao ainda nao esta autenticada
        assertFalse(solicitacao.isAuthenticated());
    }

    // verifica se a autenticacao validada e encaminhada ao JwtService
    @Test
    void deveEncaminharAutenticacaoValidadaAoJwtService() {

        // cria um DTO com credenciais ficticias
        LoginRequestDTO dto = new LoginRequestDTO(
                "gerente@raizes.local",
                "SenhaFicticiaGerente123!"
        );

        // simula a autenticacao validada pelo Spring Security
        var autenticacaoValidada = new UsernamePasswordAuthenticationToken(
                dto.email(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))
        );

        // configura o retorno do componente de autenticacao
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(autenticacaoValidada);

        // simula a resposta da geracao do token JWT
        when(jwtService.gerarToken(autenticacaoValidada))
                .thenReturn(new LoginResponseDTO(
                        "token-jwt-de-teste",
                        "Bearer",
                        3600L
                ));

        // executa o metodo responsavel pela autenticacao
        authService.autenticar(dto);

        // confirma que o componente de autenticacao foi chamado
        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );

        // garante que a mesma instancia da autenticacao validada foi encaminhada ao servico responsavel pela geracao do JWT
        verify(jwtService).gerarToken(same(autenticacaoValidada));
    }

    // verifica se credenciais invalidas impedem a geracao do JWT
    @Test
    void deveRejeitarCredenciaisInvalidas() {

        // cria um DTO com credenciais ficticias e senha incorreta
        LoginRequestDTO dto = new LoginRequestDTO(
                "admin@raizes.local",
                "SenhaIncorretaDeTeste123!"
        );

        // simula a rejeicao das credenciais pelo Spring Security
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(new BadCredentialsException(
                "Credenciais inválidas."
        ));

        // executa o servico e verifica se a excecao esperada ocorre
        BadCredentialsException excecao = assertThrows(
                BadCredentialsException.class,
                () -> authService.autenticar(dto)
        );

        // confirma a mensagem retornada pela excecao
        assertEquals("Credenciais inválidas.", excecao.getMessage());

        // verifica se houve uma tentativa de autenticacao
        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );

        // garante que o serviço de geraçao de JWT nao foi acionado
        verifyNoInteractions(jwtService);
    }

    // verifica se um usuario inativo nao consegue obter um JWT
    @Test
    void deveRejeitarUsuarioInativo() {

        // cria um DTO com credenciais ficticias de um usuario inativo
        LoginRequestDTO dto = new LoginRequestDTO(
                "inativo@raizes.local",
                "SenhaFicticiaInativo123!"
        );

        // simula a rejeicao da autenticacao devido a conta desativada
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(new DisabledException(
                "Usuário inativo."
        ));

        // executa o servico e verifica se a excecao esperada ocorre
        DisabledException excecao = assertThrows(
                DisabledException.class,
                () -> authService.autenticar(dto)
        );

        // confirma a mensagem da excecao propagada pelo servico
        assertEquals("Usuário inativo.", excecao.getMessage());

        // verifica se houve uma tentativa de autenticacao
        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );

        // garante que nenhum token JWT seja gerado
        verifyNoInteractions(jwtService);
    }

    // verifica se senhas superiores a 72 bytes são rejeitadas
    @Test
    void deveRejeitarSenhaSuperiorA72Bytes() {

        // cria uma senha ficticia contendo 73 caracteres ASCII
        String senhaLonga = "A".repeat(73);

        // confirma que a senha possui exatamente 73 bytes em UTF-8
        assertEquals(
                73,
                senhaLonga.getBytes(StandardCharsets.UTF_8).length
        );

        // cria um DTO com a senha acima do limite permitido
        LoginRequestDTO dto = new LoginRequestDTO(
                "cliente@raizes.local",
                senhaLonga
        );

        // executa o serviço e verifica se a senha e rejeitada.
        BadCredentialsException excecao = assertThrows(
                BadCredentialsException.class,
                () -> authService.autenticar(dto)
        );

        // confirma a mensagem generica de credenciais invalidas.
        assertEquals("Credenciais inválidas.", excecao.getMessage());

        // garante que a senha nao foi encaminhada para autenticacao
        verifyNoInteractions(authenticationManager);

        // garante que nenhum token JWT foi gerado
        verifyNoInteractions(jwtService);
    }

    // verifica se senhas com caracteres multibyte respeitam o limite de 72 bytes permitidos pelo BCrypt.
    @Test
    void deveRejeitarSenhaMultibyteSuperiorA72Bytes() {

        // cria uma senha ficticia com 37 caracteres acentuados
        String senhaMultibyte = "á".repeat(37);

        // confirma que a senha possui apenas 37 caracteres
        assertEquals(37, senhaMultibyte.length());

        // confirma que a senha possui 74 bytes em UTF-8
        assertEquals(
                74,
                senhaMultibyte.getBytes(StandardCharsets.UTF_8).length
        );

        // cria um DTO com a senha acima do limite de bytes
        LoginRequestDTO dto = new LoginRequestDTO(
                "cliente@raizes.local",
                senhaMultibyte
        );

        // executa o serviço e verifica se a senha e rejeitada
        BadCredentialsException excecao = assertThrows(
                BadCredentialsException.class,
                () -> authService.autenticar(dto)
        );

        // confirma a mensagem generica de credenciais invalidas
        assertEquals("Credenciais inválidas.", excecao.getMessage());

        // garante que nenhuma tentativa de autenticacao ocorreu
        verifyNoInteractions(authenticationManager);

        // garante que nenhum token JWT foi gerado
        verifyNoInteractions(jwtService);
    }
}