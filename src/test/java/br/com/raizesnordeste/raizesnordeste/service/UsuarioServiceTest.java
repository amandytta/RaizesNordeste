package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;
import br.com.raizesnordeste.raizesnordeste.repository.PerfilRepository;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioCadastroRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.entity.Perfil;
import br.com.raizesnordeste.raizesnordeste.exception.DadosInvalidosException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;


// habilita a integração entre o JUnit e o Mockito
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    // simula o repositorio de usuarios
    @Mock
    private UsuarioRepository usuarioRepository;

    // simula o repositorio de perfis
    @Mock
    private PerfilRepository perfilRepository;

    // simula o componente responsavel pela codificação das senhas
    @Mock
    private PasswordEncoder passwordEncoder;

    // instancia real do Service que será testado
    private UsuarioService usuarioService;

    // prepara uma nova instância do Service antes de cada teste
    @BeforeEach
    void setUp() {

        usuarioService = new UsuarioService(
                usuarioRepository,
                perfilRepository,
                passwordEncoder
        );
    }

    // verifica se um cliente pode ser cadastrado com dados validos
    @Test
    void deveCadastrarClienteComSucesso() {

        // arrange: prepara os dados e o comportamento das dependências
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "Cliente Teste",
                "cliente@raizes.local",
                "ClienteTeste@2026!"
        );

        // cria o perfil que será atribuido ao novo usuario
        Perfil perfilCliente = new Perfil();
        perfilCliente.setNome("CLIENTE");

        // simula a inexistencia de outro usuario com o mesmo e-mail
        when(usuarioRepository.existsByEmail(dto.email()))
                .thenReturn(false);

        // simula a localização do perfil CLIENTE
        when(perfilRepository.findByNome("CLIENTE"))
                .thenReturn(Optional.of(perfilCliente));

        // simula a codificação da senha
        when(passwordEncoder.encode(dto.senha()))
                .thenReturn("hash-simulado");

        // simula a persistencia retornando o usuario recebido
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act: executa o método real do Service
        UsuarioResponseDTO resposta = usuarioService.cadastrarCliente(dto);

        // assert: verifica os dados retornados pelo cadastro
        assertNotNull(resposta);
        assertEquals("Cliente Teste", resposta.nome());
        assertEquals("cliente@raizes.local", resposta.email());
        assertTrue(resposta.ativo());
        assertEquals(Set.of("CLIENTE"), resposta.perfis());

        // confirma as interações com as dependencias
        verify(usuarioRepository).existsByEmail(dto.email());
        verify(perfilRepository).findByNome("CLIENTE");
        verify(passwordEncoder).encode(dto.senha());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    // verifica se os dados do cliente sao normalizados antes da persistência
    @Test
    void deveNormalizarDadosDoCliente() {

        // arrange: prepara os dados com espaços e letras maiusculas
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "  Cliente Teste  ",
                "CLIENTE@RAIZES.LOCAL",
                "ClienteTeste@2026!"
        );

        // cria o perfil padrão do cliente
        Perfil perfilCliente = new Perfil();
        perfilCliente.setNome("CLIENTE");

        // simula a verificação de duplicidade com o e-mail normalizado
        when(usuarioRepository.existsByEmail("cliente@raizes.local"))
                .thenReturn(false);

        // simula a localização do perfil CLIENTE
        when(perfilRepository.findByNome("CLIENTE"))
                .thenReturn(Optional.of(perfilCliente));

        // simula a codificação da senha
        when(passwordEncoder.encode(dto.senha()))
                .thenReturn("hash-simulado");

        // simula a persistencia do usuario
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act: executa o cadastro do cliente
        UsuarioResponseDTO resposta = usuarioService.cadastrarCliente(dto);

        // assert: verifica se os dados foram normalizados corretamente
        assertEquals("Cliente Teste", resposta.nome());
        assertEquals("cliente@raizes.local", resposta.email());

        // confirma que a verificação de duplicidade utiliza o e-mail normalizado
        verify(usuarioRepository).existsByEmail("cliente@raizes.local");

        // garante que o e-mail original nao foi utilizado nessa verificação
        verify(usuarioRepository, never()).existsByEmail(dto.email());

        // confirma que o usuario foi enviado para persistencia
        verify(usuarioRepository).save(any(Usuario.class));
    }

    // verifica se o cadastro com e-mail duplicado e rejeitado
    @Test
    void deveRejeitarCadastroComEmailDuplicado() {

        // arrange: prepara os dados ficticios do cliente
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "Cliente Teste",
                "cliente@raizes.local",
                "ClienteTeste@2026!"
        );

        // simula a existência de um usuario com o mesmo e-mail
        when(usuarioRepository.existsByEmail(dto.email()))
                .thenReturn(true);

        // act: executa o cadastro e captura a excecao esperada
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.cadastrarCliente(dto)
        );

        // assert: verifica a mensagem da exceção
        assertEquals(
                "Já existe um usuário cadastrado com este e-mail.",
                excecao.getMessage()
        );

        // confirma que a verificação de duplicidade foi realizada.=
        verify(usuarioRepository).existsByEmail(dto.email());

        // garante que nenhum usuario foi enviado para persistencia
        verify(usuarioRepository, never()).save(any(Usuario.class));

        // garante que o perfil não foi consultado e que nenhuma senha foi codificada
        verifyNoInteractions(perfilRepository, passwordEncoder);
    }

    // verifica se a duplicidade e identificada apos a normalização do e-mail
    @Test
    void deveRejeitarEmailDuplicadoAposNormalizacao() {

        // arrange: prepara os dados com um e-mail em maiusculas
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "Cliente Teste",
                "CLIENTE@RAIZES.LOCAL",
                "ClienteTeste@2026!"
        );

        // simula a existencia do e-mail em seu formato normalizado
        when(usuarioRepository.existsByEmail("cliente@raizes.local"))
                .thenReturn(true);

        // act: executa o cadastro e captura a excecao esperada
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.cadastrarCliente(dto)
        );

        // assert: verifica a mensagem da exceção
        assertEquals(
                "Já existe um usuário cadastrado com este e-mail.",
                excecao.getMessage()
        );

        // confirma que a consulta utiliza o e-mail normalizado
        verify(usuarioRepository).existsByEmail("cliente@raizes.local");

        // garante que o e-mail original não foi utilizado na consulta
        verify(usuarioRepository, never()).existsByEmail(dto.email());

        // garante que nenhum usuario foi enviado para persistência
        verify(usuarioRepository, never()).save(any(Usuario.class));

        // garante que o fluxo foi interrompido antes das demais operações
        verifyNoInteractions(perfilRepository, passwordEncoder);
    }

    // verifica se uma senha superior a 72 bytes e rejeitada
    @Test
    void deveRejeitarSenhaSuperiorA72Bytes() {

        // arrange: cria uma senha com 73 caracteres ASCII
        String senha = "A".repeat(73);

        // confirma que a senha possui 73 bytes em UTF-8
        assertEquals(73, senha.getBytes(StandardCharsets.UTF_8).length);

        // prepara os dados ficticios do cliente
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "Cliente Teste",
                "cliente@raizes.local",
                senha
        );

        // simula a inexistencia de outro usuario com o mesmo e-mail
        when(usuarioRepository.existsByEmail(dto.email()))
                .thenReturn(false);

        // act: executa o cadastro e captura a excecao esperada
        DadosInvalidosException excecao = assertThrows(
                DadosInvalidosException.class,
                () -> usuarioService.cadastrarCliente(dto)
        );

        // assert: verifica a mensagem da exceção
        assertEquals(
                "A senha deve possuir no máximo 72 bytes.",
                excecao.getMessage()
        );

        // confirma que a verificação de duplicidade foi realizada
        verify(usuarioRepository).existsByEmail(dto.email());

        // garante que nenhum usuario foi enviado para persistencia
        verify(usuarioRepository, never()).save(any(Usuario.class));

        // garante que o perfil não foi consultado e que a senha invalida não foi codificada
        verifyNoInteractions(perfilRepository, passwordEncoder);
    }

    // verifica se uma senha multibyte superior a 72 bytes e rejeitada
    @Test
    void deveRejeitarSenhaMultibyteSuperiorA72Bytes() {

        // arrange: cria uma senha com 37 caracteres acentuados
        String senha = "á".repeat(37);

        // confirma a quantidade de caracteres da senha
        assertEquals(37, senha.length());

        // confirma que a senha possui 74 bytes em UTF-8
        assertEquals(74, senha.getBytes(StandardCharsets.UTF_8).length);

        // prepara os dados ficticios do cliente
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "Cliente Teste",
                "cliente@raizes.local",
                senha
        );

        // simula a inexistencia de outro usuario com o mesmo e-mail
        when(usuarioRepository.existsByEmail(dto.email()))
                .thenReturn(false);

        // act: executa o cadastro e captura a exceção esperada
        DadosInvalidosException excecao = assertThrows(
                DadosInvalidosException.class,
                () -> usuarioService.cadastrarCliente(dto)
        );

        // assert: verifica a mensagem da exceção
        assertEquals(
                "A senha deve possuir no máximo 72 bytes.",
                excecao.getMessage()
        );

        // confirma que a verificação de duplicidade foi realizada
        verify(usuarioRepository).existsByEmail(dto.email());

        // garante que nenhum usuario foi enviado para persistencia
        verify(usuarioRepository, never()).save(any(Usuario.class));

        // garante que o perfil não foi consultado e que a senha invalida não foi codificada
        verifyNoInteractions(perfilRepository, passwordEncoder);
    }

    // verifica se uma senha com exatamente 72 bytes e aceita
    @Test
    void deveAceitarSenhaComExatamente72Bytes() {

        // arrange: cria uma senha com 36 caracteres multibyte
        String senha = "á".repeat(36);

        // confirma a quantidade de caracteres da senha
        assertEquals(36, senha.length());

        // confirma que a senha possui exatamente 72 bytes em UTF-8
        assertEquals(72, senha.getBytes(StandardCharsets.UTF_8).length);

        // prepara os dados ficticios do cliente
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "Cliente Teste",
                "cliente@raizes.local",
                senha
        );

        // cria o perfil padrão do cliente
        Perfil perfilCliente = new Perfil();
        perfilCliente.setNome("CLIENTE");

        // simula a inexistencia de outro usuario com o mesmo e-mail
        when(usuarioRepository.existsByEmail(dto.email()))
                .thenReturn(false);

        // simula a localização do perfil CLIENTE
        when(perfilRepository.findByNome("CLIENTE"))
                .thenReturn(Optional.of(perfilCliente));

        // simula a codificação da senha
        when(passwordEncoder.encode(senha))
                .thenReturn("hash-simulado");

        // simula a persistencia do usuario
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act: executa o cadastro e garante que nenhuma exceção seja lançada
        UsuarioResponseDTO resposta = assertDoesNotThrow(
                () -> usuarioService.cadastrarCliente(dto)
        );

        // assert: verifica os dados retornados pelo cadastro
        assertNotNull(resposta);
        assertEquals(dto.nome(), resposta.nome());
        assertEquals(dto.email(), resposta.email());
        assertTrue(resposta.ativo());
        assertEquals(Set.of("CLIENTE"), resposta.perfis());

        // confirma que a senha foi encaminhada para codificação
        verify(passwordEncoder).encode(senha);

        // confirma que o usuario foi enviado para persistencia
        verify(usuarioRepository).save(any(Usuario.class));
    }

    // verifica se o cadastro e interrompido quando o perfil CLIENTE não existe
    @Test
    void deveRejeitarCadastroQuandoPerfilClienteNaoExiste() {

        // arrange: prepara os dados ficticios do cliente
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "Cliente Teste",
                "cliente@raizes.local",
                "ClienteTeste@2026!"
        );

        // simula a inexistencia de outro usuario com o mesmo e-mail
        when(usuarioRepository.existsByEmail(dto.email()))
                .thenReturn(false);

        // simula a ausência do perfil CLIENTE no banco de dados
        when(perfilRepository.findByNome("CLIENTE"))
                .thenReturn(Optional.empty());

        // act: executa o cadastro e captura a exceção esperada
        IllegalStateException excecao = assertThrows(
                IllegalStateException.class,
                () -> usuarioService.cadastrarCliente(dto)
        );

        // assert: verifica a mensagem da exceção
        assertEquals(
                "Perfil CLIENTE não encontrado.",
                excecao.getMessage()
        );

        // confirma que a verificação de duplicidade foi realizada
        verify(usuarioRepository).existsByEmail(dto.email());

        // confirma que o Service tentou localizar o perfil CLIENTE
        verify(perfilRepository).findByNome("CLIENTE");

        // garante que nenhuma senha foi codificada
        verifyNoInteractions(passwordEncoder);

        // garante que nenhum usuario foi enviado para persistencia
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // verifica se a senha codificada e utilizada na persistencia do usuario
    @Test
    void devePersistirUsuarioComSenhaCodificada() {

        // arrange: prepara os dados ficticios do cliente
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "Cliente Teste",
                "cliente@raizes.local",
                "ClienteTeste@2026!"
        );

        // define o hash que será retornado pelo codificador simulado
        String senhaCodificada = "hash-simulado";

        // cria o perfil padrão do cliente
        Perfil perfilCliente = new Perfil();
        perfilCliente.setNome("CLIENTE");

        // simula a inexistencia de outro usuario com o mesmo e-mail
        when(usuarioRepository.existsByEmail(dto.email()))
                .thenReturn(false);

        // simula a localização do perfil CLIENTE
        when(perfilRepository.findByNome("CLIENTE"))
                .thenReturn(Optional.of(perfilCliente));

        // simula a codificação da senha original
        when(passwordEncoder.encode(dto.senha()))
                .thenReturn(senhaCodificada);

        // simula a persistencia do usuario
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act: executa o cadastro do client
        usuarioService.cadastrarCliente(dto);

        // cria um capturador para obter o usuario enviado ao repositorio
        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        // captura o objeto recebido pelo metodo save()
        verify(usuarioRepository).save(captor.capture());

        // recupera o usuario capturado
        Usuario usuarioCapturado = captor.getValue();

        // assert: confirma que o hash retornado pelo codificador foi utilizado
        assertEquals(senhaCodificada, usuarioCapturado.getSenhaHash());

        // garante que a senha original não foi utilizada na persistencia
        assertNotEquals(dto.senha(), usuarioCapturado.getSenhaHash());

        // confirma que a senha original foi encaminhada ao codificador
        verify(passwordEncoder).encode(dto.senha());
    }

    // verifica se o usuario e cadastrado como ativo e recebe exclusivamente o perfil CLIENTE
    @Test
    void deveCadastrarUsuarioAtivoComPerfilExclusivamenteCliente() {

        // arrange: prepara os dados ficticios do cliente
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "Cliente Teste",
                "cliente@raizes.local",
                "ClienteTeste@2026!"
        );

        // cria o perfil permitido para o cadastro publico
        Perfil perfilCliente = new Perfil();
        perfilCliente.setNome("CLIENTE");

        // simula a inexistencia de outro usuario com o mesmo e-mail
        when(usuarioRepository.existsByEmail(dto.email()))
                .thenReturn(false);

        // simula a localizacao do perfil CLIENTE
        when(perfilRepository.findByNome("CLIENTE"))
                .thenReturn(Optional.of(perfilCliente));

        // simula a codificação da senha
        when(passwordEncoder.encode(dto.senha()))
                .thenReturn("hash-simulado");

        // simula a persistência do usuario
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // act: executa o cadastro do cliente
        UsuarioResponseDTO resposta = usuarioService.cadastrarCliente(dto);

        // captura a entidade enviada para persistencia
        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        // recupera o usuario criado pelo Service
        Usuario usuarioCapturado = captor.getValue();

        // assert: garante que o usuario foi cadastrado como ativo
        assertTrue(usuarioCapturado.getAtivo());

        // garante que o usuario possui exatamente um perfil
        assertEquals(1, usuarioCapturado.getPerfis().size());

        // garante que o unico perfil atribuido e o perfil CLIENTE
        assertEquals(
                Set.of(perfilCliente),
                usuarioCapturado.getPerfis()
        );

        // confirma que a resposta também apresenta os dados corretos
        assertTrue(resposta.ativo());
        assertEquals(Set.of("CLIENTE"), resposta.perfis());

        // confirma que somente o perfil CLIENTE foi consultado
        verify(perfilRepository).findByNome("CLIENTE");
        verifyNoMoreInteractions(perfilRepository);
    }

    // verifica se a resposta apresenta os dados do usuario persistido
    @Test
    void deveRetornarDadosDoUsuarioPersistido() {

        // arrange: prepara os dados ficticios do cliente
        UsuarioCadastroRequestDTO dto = new UsuarioCadastroRequestDTO(
                "Cliente Teste",
                "cliente@raizes.local",
                "ClienteTeste@2026!"
        );

        // cria o perfil padrão do cliente
        Perfil perfilCliente = new Perfil();
        perfilCliente.setNome("CLIENTE");

        // simula a inexistencia de outro usuario com o mesmo e-mail
        when(usuarioRepository.existsByEmail(dto.email()))
                .thenReturn(false);

        // simula a localização do perfil CLIENTE
        when(perfilRepository.findByNome("CLIENTE"))
                .thenReturn(Optional.of(perfilCliente));

        // simula a codificação da senha
        when(passwordEncoder.encode(dto.senha()))
                .thenReturn("hash-simulado");

        // cria uma entidade representando o usuario persistido
        Usuario usuarioSalvo = new Usuario();

        // simula o identificador gerado pelo banco de dados
        ReflectionTestUtils.setField(usuarioSalvo, "id", 1L);

        // preenche os atributos do usuario persistido
        usuarioSalvo.setNome(dto.nome());
        usuarioSalvo.setEmail(dto.email());
        usuarioSalvo.setSenhaHash("hash-simulado");
        usuarioSalvo.setAtivo(true);
        usuarioSalvo.setPerfis(Set.of(perfilCliente));

        // simula o retorno da entidade persistida pelo repositorio
        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuarioSalvo);

        // act: executa o cadastro do cliente
        UsuarioResponseDTO resposta = usuarioService.cadastrarCliente(dto);

        // assert: verifica todos os atributos do DTO de resposta
        assertNotNull(resposta);
        assertEquals(1L, resposta.id());
        assertEquals(dto.nome(), resposta.nome());
        assertEquals(dto.email(), resposta.email());
        assertTrue(resposta.ativo());
        assertEquals(Set.of("CLIENTE"), resposta.perfis());

        // confirma que o usuario foi enviado para persistencia
        verify(usuarioRepository).save(any(Usuario.class));
    }
}