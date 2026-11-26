package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.entity.Perfil;
import br.com.raizesnordeste.raizesnordeste.entity.Unidade;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.repository.UnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioUnidadeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


// testa isoladamente as regras de autorização por unidade
@ExtendWith(MockitoExtension.class)
class PermissaoUnidadeServiceTest {

    // define o e-mail utilizado nos cenários de teste
    private static final String EMAIL_TESTE =
            "usuario@raizes.local";

    // simula o repository de usuários
    @Mock
    private UsuarioRepository usuarioRepository;

    // simula o repository de unidades
    @Mock
    private UnidadeRepository unidadeRepository;

    // simula o repository de vínculos
    @Mock
    private UsuarioUnidadeRepository usuarioUnidadeRepository;

    // serviço responsável pelas validações que serão testadas
    private PermissaoUnidadeService permissaoUnidadeService;

    // inicializa o serviço antes de cada teste
    @BeforeEach
    void setUp() {

        permissaoUnidadeService = new PermissaoUnidadeService(
                usuarioRepository,
                unidadeRepository,
                usuarioUnidadeRepository
        );
    }

    // cria um usuário simulado com o perfil e a situação informados
    private Usuario criarUsuario(String nomePerfil, boolean ativo) {

        // prepara o perfil do usuário
        Perfil perfil = new Perfil();
        perfil.setNome(nomePerfil);

        // prepara a entidade de usuário
        Usuario usuario = new Usuario();

        // simula o identificador de uma entidade persistida
        ReflectionTestUtils.setField(usuario, "id", 1L);

        usuario.setNome("Usuário Teste");
        usuario.setEmail(EMAIL_TESTE);
        usuario.setAtivo(ativo);
        usuario.setPerfis(Set.of(perfil));

        return usuario;
    }

    // cria uma unidade simulada com a situação informada
    private Unidade criarUnidade(Long id, boolean ativa) {

        Unidade unidade = new Unidade();

        // simula o identificador da unidade persistida
        ReflectionTestUtils.setField(unidade, "id", id);

        unidade.setNome("Unidade Teste " + id);
        unidade.setAtiva(ativa);

        return unidade;
    }

    // teste 01: permite acesso administrativo sem vínculo
    @Test
    void devePermitirAcessoDeAdministradorSemVinculo() {

        Usuario usuario = criarUsuario("ADMIN", true);
        Unidade unidade = criarUnidade(1L, true);

        // simula a existência do administrador e da unidade
        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        // verifica se o administrador possui acesso global
        assertDoesNotThrow(() ->
                permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        1L
                )
        );

        // garante que administradores não precisam de vínculos
        verifyNoInteractions(usuarioUnidadeRepository);

        verify(unidadeRepository).findById(1L);
    }

    // teste 02: permite acesso de atendente com vínculo ativo
    @Test
    void devePermitirAcessoDeAtendenteComVinculoAtivo() {

        Usuario usuario = criarUsuario("ATENDENTE", true);
        Unidade unidade = criarUnidade(1L, true);

        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        // simula um vínculo ativo com a unidade solicitada
        when(usuarioUnidadeRepository
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 1L))
                .thenReturn(true);

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        // verifica se o acesso é autorizado
        assertDoesNotThrow(() ->
                permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        1L
                )
        );

        // confirma a consulta do vínculo correto
        verify(usuarioUnidadeRepository)
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 1L);
    }

    // teste 03: permite acesso de gerente a múltiplas unidades vinculadas
    @Test
    void devePermitirAcessoDeGerenteAMultiplasUnidades() {

        Usuario usuario = criarUsuario("GERENTE", true);

        Unidade unidade1 = criarUnidade(1L, true);
        Unidade unidade2 = criarUnidade(2L, true);

        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        // simula dois vínculos ativos para o mesmo gerente
        when(usuarioUnidadeRepository
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 1L))
                .thenReturn(true);

        when(usuarioUnidadeRepository
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 2L))
                .thenReturn(true);

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade1));

        when(unidadeRepository.findById(2L))
                .thenReturn(Optional.of(unidade2));

        // verifica o acesso às duas unidades
        assertDoesNotThrow(() ->
                permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        1L
                )
        );

        assertDoesNotThrow(() ->
                permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        2L
                )
        );

        // confirma a validação individual de cada vínculo
        verify(usuarioUnidadeRepository)
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 1L);

        verify(usuarioUnidadeRepository)
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 2L);
    }

    // teste 04: impede acesso de atendente sem vínculo ativo
    @Test
    void deveNegarAcessoDeAtendenteSemVinculoAtivo() {

        Usuario usuario = criarUsuario("ATENDENTE", true);

        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        // simula a ausência de vínculo ativo
        when(usuarioUnidadeRepository
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 2L))
                .thenReturn(false);

        // verifica se o acesso é negado
        AccessDeniedException excecao = assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        2L
                )
        );

        assertEquals(
                "O usuário não possui vínculo ativo com esta unidade.",
                excecao.getMessage()
        );

        // garante que a unidade não foi consultada sem autorização
        verifyNoInteractions(unidadeRepository);
    }

    // teste 05: impede acesso de gerente a uma unidade não vinculada
    @Test
    void deveNegarAcessoDeGerenteAUnidadeNaoVinculada() {

        Usuario usuario = criarUsuario("GERENTE", true);

        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        // simula uma unidade fora da autorização do gerente
        when(usuarioUnidadeRepository
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 3L))
                .thenReturn(false);

        AccessDeniedException excecao = assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        3L
                )
        );

        assertEquals(
                "O usuário não possui vínculo ativo com esta unidade.",
                excecao.getMessage()
        );

        verifyNoInteractions(unidadeRepository);
    }

    // teste 06: impede acesso de clientes às operações internas
    @Test
    void deveNegarAcessoDeCliente() {

        Usuario usuario = criarUsuario("CLIENTE", true);

        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        // verifica a rejeição do perfil de cliente
        AccessDeniedException excecao = assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        1L
                )
        );

        assertEquals(
                "O usuário não possui permissão para operar em unidades.",
                excecao.getMessage()
        );

        // garante que a operação foi interrompida
        verifyNoInteractions(
                unidadeRepository,
                usuarioUnidadeRepository
        );
    }

    // teste 07: impede acesso de funcionários inativos
    @Test
    void deveNegarAcessoDeFuncionarioInativo() {

        Usuario usuario = criarUsuario("ATENDENTE", false);

        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        // verifica se a situação atual do usuário impede o acesso
        AccessDeniedException excecao = assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        1L
                )
        );

        assertEquals(
                "O usuário está inativo.",
                excecao.getMessage()
        );

        verifyNoInteractions(
                unidadeRepository,
                usuarioUnidadeRepository
        );
    }

    // teste 08: impede acesso de administradores inativos
    @Test
    void deveNegarAcessoDeAdministradorInativo() {

        Usuario usuario = criarUsuario("ADMIN", false);

        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        // garante que o perfil ADMIN não ignora a situação do usuário
        AccessDeniedException excecao = assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        1L
                )
        );

        assertEquals(
                "O usuário está inativo.",
                excecao.getMessage()
        );

        verifyNoInteractions(
                unidadeRepository,
                usuarioUnidadeRepository
        );
    }

    // teste 09: impede acesso de usuários inexistentes
    @Test
    void deveNegarAcessoDeUsuarioInexistente() {

        // simula um usuário que não existe mais no banco de dados
        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.empty());

        AccessDeniedException excecao = assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        1L
                )
        );

        assertEquals(
                "Usuário sem permissão de acesso.",
                excecao.getMessage()
        );

        verifyNoInteractions(
                unidadeRepository,
                usuarioUnidadeRepository
        );
    }

    // teste 10: impede operações em unidades inativas
    @Test
    void deveNegarAcessoAUnidadeInativa() {

        Usuario usuario = criarUsuario("GERENTE", true);
        Unidade unidade = criarUnidade(1L, false);

        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        // simula um vínculo ativo com uma unidade inativa
        when(usuarioUnidadeRepository
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 1L))
                .thenReturn(true);

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        // verifica se o vínculo ativo é insuficiente para autorizar o acesso
        AccessDeniedException excecao = assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        1L
                )
        );

        assertEquals(
                "A unidade está inativa.",
                excecao.getMessage()
        );
    }

    // teste 11: rejeita a consulta administrativa de unidade inexistente
    @Test
    void deveRejeitarAcessoAdministrativoAUnidadeInexistente() {

        Usuario usuario = criarUsuario("ADMIN", true);

        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        // simula uma unidade inexistente
        when(unidadeRepository.findById(999L))
                .thenReturn(Optional.empty());

        NoSuchElementException excecao = assertThrows(
                NoSuchElementException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        999L
                )
        );

        assertEquals(
                "Unidade não encontrada.",
                excecao.getMessage()
        );

        verifyNoInteractions(usuarioUnidadeRepository);
    }

    // teste 12: impede acesso após a desativação de um vínculo
    @Test
    void deveNegarAcessoAposDesativacaoDoVinculo() {

        Usuario usuario = criarUsuario("ATENDENTE", true);
        Unidade unidade = criarUnidade(1L, true);

        when(usuarioRepository.findByEmail(EMAIL_TESTE))
                .thenReturn(Optional.of(usuario));

        // simula um vínculo inicialmente ativo e posteriormente desativado
        when(usuarioUnidadeRepository
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 1L))
                .thenReturn(true, false);

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        // verifica se o primeiro acesso é autorizado
        assertDoesNotThrow(() ->
                permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        1L
                )
        );

        // verifica se o segundo acesso é negado após a desativação
        AccessDeniedException excecao = assertThrows(
                AccessDeniedException.class,
                () -> permissaoUnidadeService.validarAcesso(
                        EMAIL_TESTE,
                        1L
                )
        );

        assertEquals(
                "O usuário não possui vínculo ativo com esta unidade.",
                excecao.getMessage()
        );

        // confirma que a autorização foi consultada novamente
        verify(usuarioUnidadeRepository, times(2))
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(1L, 1L);

        // confirma que a unidade foi consultada apenas no acesso autorizado
        verify(unidadeRepository, times(1)).findById(1L);
    }
}