package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.UsuarioUnidadeRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioUnidadeResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Perfil;
import br.com.raizesnordeste.raizesnordeste.entity.Unidade;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.entity.UsuarioUnidade;
import br.com.raizesnordeste.raizesnordeste.entity.UsuarioUnidadeId;
import br.com.raizesnordeste.raizesnordeste.repository.UnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioUnidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// testa isoladamente as regras de gerenciamento de vínculos
@ExtendWith(MockitoExtension.class)
class UsuarioUnidadeServiceTest {

    // simula o repository de vínculos
    @Mock
    private UsuarioUnidadeRepository usuarioUnidadeRepository;

    // simula o repository de usuários
    @Mock
    private UsuarioRepository usuarioRepository;

    // simula o repository de unidades
    @Mock
    private UnidadeRepository unidadeRepository;

    // serviço que será testado
    private UsuarioUnidadeService usuarioUnidadeService;

    // inicializa o serviço antes de cada teste
    @Mock
    private AuditoriaService auditoriaService;

    @BeforeEach
    void setUp() {
        usuarioUnidadeService = new UsuarioUnidadeService(
                usuarioUnidadeRepository,
                usuarioRepository,
                unidadeRepository,
                auditoriaService
        );
    }

    // simula o administrador responsável apenas nos cenários administrativos bem-sucedidos
    private void autenticarAdministrador() {
        Usuario administrador = new Usuario();
        ReflectionTestUtils.setField(administrador, "id", 99L);
        administrador.setEmail("admin.teste@raizes.local");
        administrador.setAtivo(true);
        Perfil perfil = new Perfil();
        perfil.setNome("ADMIN");
        administrador.setPerfis(Set.of(perfil));
        when(usuarioRepository.findByEmail(administrador.getEmail()))
                .thenReturn(Optional.of(administrador));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(administrador.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    // evita que a autenticação simulada seja reutilizada por outro teste
    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }


    // cria um usuário simulado com o perfil e a situação informados
    private Usuario criarUsuario(String nomePerfil, boolean ativo) {

        Perfil perfil = new Perfil();
        perfil.setNome(nomePerfil);

        Usuario usuario = new Usuario();

        // define o identificador para simular uma entidade persistida
        ReflectionTestUtils.setField(usuario, "id", 1L);

        usuario.setNome("Usuário Teste");
        usuario.setEmail("usuario@raizes.local");
        usuario.setAtivo(ativo);
        usuario.setPerfis(Set.of(perfil));

        return usuario;
    }

    // cria uma unidade simulada com a situação informada
    private Unidade criarUnidade(Long id, boolean ativa) {

        Unidade unidade = new Unidade();

        // define o identificador da unidade simulada
        ReflectionTestUtils.setField(unidade, "id", id);

        unidade.setNome("Unidade Teste " + id);
        unidade.setAtiva(ativa);

        return unidade;
    }

    // cria um vínculo existente para utilização nos testes
    private UsuarioUnidade criarVinculo(
            Usuario usuario,
            Unidade unidade,
            boolean ativo) {

        UsuarioUnidade vinculo =
                new UsuarioUnidade(usuario, unidade);

        vinculo.setAtivo(ativo);

        // simula a definição da data realizada pelo JPA
        vinculo.prePersist();

        return vinculo;
    }

    // teste 01: permite vincular um atendente à primeira unidade
    @Test
    void deveVincularAtendenteComSucesso() {

        autenticarAdministrador();

        Usuario usuario = criarUsuario("ATENDENTE", true);
        Unidade unidade = criarUnidade(1L, true);

        UsuarioUnidadeRequestDTO dto =
                new UsuarioUnidadeRequestDTO(1L);

        // simula a consulta do usuário com bloqueio
        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        // simula a ausência de vínculos anteriores
        when(usuarioUnidadeRepository.findById(
                new UsuarioUnidadeId(1L, 1L)))
                .thenReturn(Optional.empty());

        when(usuarioUnidadeRepository.countByUsuarioIdAndAtivoTrue(1L))
                .thenReturn(0L);

        // simula a persistência do novo vínculo
        when(usuarioUnidadeRepository.save(any(UsuarioUnidade.class)))
                .thenAnswer(invocation -> {
                    UsuarioUnidade vinculo = invocation.getArgument(0);
                    vinculo.prePersist();
                    return vinculo;
                });

        // executa a operação
        UsuarioUnidadeResponseDTO resposta =
                usuarioUnidadeService.vincular(1L, dto);

        // verifica os dados do vínculo criado
        assertEquals(1L, resposta.usuarioId());
        assertEquals(1L, resposta.unidadeId());
        assertTrue(resposta.ativo());
        assertNotNull(resposta.dataVinculo());

        // confirma a utilização do bloqueio e a persistência
        verify(usuarioRepository).findByIdComBloqueio(1L);
        verify(usuarioUnidadeRepository).save(any(UsuarioUnidade.class));
    }

    // teste 02: impede que um atendente possua duas unidades ativas
    @Test
    void deveRejeitarSegundoVinculoAtivoDeAtendente() {

        Usuario usuario = criarUsuario("ATENDENTE", true);
        Unidade unidade = criarUnidade(2L, true);

        // simula um atendente que já possui vínculo ativo
        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        when(unidadeRepository.findById(2L))
                .thenReturn(Optional.of(unidade));

        when(usuarioUnidadeRepository.findById(
                new UsuarioUnidadeId(1L, 2L)))
                .thenReturn(Optional.empty());

        when(usuarioUnidadeRepository.countByUsuarioIdAndAtivoTrue(1L))
                .thenReturn(1L);

        // verifica a rejeição do segundo vínculo
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioUnidadeService.vincular(
                        1L,
                        new UsuarioUnidadeRequestDTO(2L)
                )
        );

        assertEquals(
                "O atendente já possui vínculo ativo com outra unidade.",
                excecao.getMessage()
        );

        // garante que nenhum vínculo foi persistido
        verify(usuarioUnidadeRepository, never())
                .save(any(UsuarioUnidade.class));
    }

    // teste 03: permite que um gerente seja associado a outra unidade
    @Test
    void devePermitirMultiplosVinculosParaGerente() {

        autenticarAdministrador();

        Usuario usuario = criarUsuario("GERENTE", true);
        Unidade unidade = criarUnidade(2L, true);

        // simula a consulta do gerente e da nova unidade
        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        when(unidadeRepository.findById(2L))
                .thenReturn(Optional.of(unidade));

        when(usuarioUnidadeRepository.findById(
                new UsuarioUnidadeId(1L, 2L)))
                .thenReturn(Optional.empty());

        // simula a persistência do vínculo
        when(usuarioUnidadeRepository.save(any(UsuarioUnidade.class)))
                .thenAnswer(invocation -> {
                    UsuarioUnidade vinculo = invocation.getArgument(0);
                    vinculo.prePersist();
                    return vinculo;
                });

        // executa a associação
        UsuarioUnidadeResponseDTO resposta =
                usuarioUnidadeService.vincular(
                        1L,
                        new UsuarioUnidadeRequestDTO(2L)
                );

        assertEquals(2L, resposta.unidadeId());
        assertTrue(resposta.ativo());

        // confirma que a restrição de quantidade não se aplica ao gerente
        verify(usuarioUnidadeRepository, never())
                .countByUsuarioIdAndAtivoTrue(anyLong());

        verify(usuarioUnidadeRepository)
                .save(any(UsuarioUnidade.class));
    }

    // teste 04: impede a vinculação de clientes
    @Test
    void deveRejeitarVinculoDeCliente() {

        Usuario usuario = criarUsuario("CLIENTE", true);

        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        // verifica a rejeição do perfil incompatível
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioUnidadeService.vincular(
                        1L,
                        new UsuarioUnidadeRequestDTO(1L)
                )
        );

        assertEquals(
                "Somente atendentes e gerentes podem ser vinculados a unidades.",
                excecao.getMessage()
        );

        // garante que a operação não avançou
        verifyNoInteractions(
                unidadeRepository,
                usuarioUnidadeRepository
        );
    }

    // teste 05: impede a vinculação de administradores
    @Test
    void deveRejeitarVinculoDeAdministrador() {

        Usuario usuario = criarUsuario("ADMIN", true);

        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        // verifica a rejeição do perfil administrativo
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioUnidadeService.vincular(
                        1L,
                        new UsuarioUnidadeRequestDTO(1L)
                )
        );

        assertEquals(
                "Somente atendentes e gerentes podem ser vinculados a unidades.",
                excecao.getMessage()
        );

        verifyNoInteractions(
                unidadeRepository,
                usuarioUnidadeRepository
        );
    }

    // teste 06: impede a vinculação de usuários inativos
    @Test
    void deveRejeitarVinculoDeUsuarioInativo() {

        Usuario usuario = criarUsuario("ATENDENTE", false);

        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        // verifica a rejeição do usuário inativo
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioUnidadeService.vincular(
                        1L,
                        new UsuarioUnidadeRequestDTO(1L)
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

    // teste 07: impede a associação com unidades inativas
    @Test
    void deveRejeitarVinculoComUnidadeInativa() {

        Usuario usuario = criarUsuario("GERENTE", true);
        Unidade unidade = criarUnidade(1L, false);

        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        // verifica a rejeição da unidade inativa
        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioUnidadeService.vincular(
                        1L,
                        new UsuarioUnidadeRequestDTO(1L)
                )
        );

        assertEquals(
                "A unidade está inativa.",
                excecao.getMessage()
        );

        verifyNoInteractions(usuarioUnidadeRepository);
    }

    // teste 08: impede a duplicação de um vínculo ativo
    @Test
    void deveRejeitarVinculoAtivoDuplicado() {

        Usuario usuario = criarUsuario("GERENTE", true);
        Unidade unidade = criarUnidade(1L, true);

        UsuarioUnidade vinculo =
                criarVinculo(usuario, unidade, true);

        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        // simula um vínculo que já está ativo
        when(usuarioUnidadeRepository.findById(
                new UsuarioUnidadeId(1L, 1L)))
                .thenReturn(Optional.of(vinculo));

        IllegalArgumentException excecao = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioUnidadeService.vincular(
                        1L,
                        new UsuarioUnidadeRequestDTO(1L)
                )
        );

        assertEquals(
                "O usuário já possui vínculo ativo com esta unidade.",
                excecao.getMessage()
        );

        verify(usuarioUnidadeRepository, never())
                .save(any(UsuarioUnidade.class));
    }

    // teste 09: permite reativar um vínculo anteriormente desativado
    @Test
    void deveReativarVinculoInativo() {

        autenticarAdministrador();

        Usuario usuario = criarUsuario("ATENDENTE", true);
        Unidade unidade = criarUnidade(1L, true);

        UsuarioUnidade vinculo =
                criarVinculo(usuario, unidade, false);

        // armazena a data original para verificar sua preservação
        LocalDateTime dataOriginal = vinculo.getDataVinculo();

        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        when(unidadeRepository.findById(1L))
                .thenReturn(Optional.of(unidade));

        when(usuarioUnidadeRepository.findById(
                new UsuarioUnidadeId(1L, 1L)))
                .thenReturn(Optional.of(vinculo));

        when(usuarioUnidadeRepository.countByUsuarioIdAndAtivoTrue(1L))
                .thenReturn(0L);

        when(usuarioUnidadeRepository.save(vinculo))
                .thenReturn(vinculo);

        // executa a reativação
        UsuarioUnidadeResponseDTO resposta =
                usuarioUnidadeService.vincular(
                        1L,
                        new UsuarioUnidadeRequestDTO(1L)
                );

        // confirma a reativação e a preservação da data original
        assertTrue(resposta.ativo());
        assertEquals(dataOriginal, resposta.dataVinculo());

        verify(usuarioUnidadeRepository).save(vinculo);
    }

    // teste 10: permite desativar um vínculo existente
    @Test
    void deveDesvincularUsuarioComSucesso() {

        autenticarAdministrador();

        Usuario usuario = criarUsuario("ATENDENTE", true);
        Unidade unidade = criarUnidade(1L, true);

        UsuarioUnidade vinculo =
                criarVinculo(usuario, unidade, true);

        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        when(usuarioUnidadeRepository.findById(
                new UsuarioUnidadeId(1L, 1L)))
                .thenReturn(Optional.of(vinculo));

        when(usuarioUnidadeRepository.save(vinculo))
                .thenReturn(vinculo);

        // executa a desvinculação
        UsuarioUnidadeResponseDTO resposta =
                usuarioUnidadeService.desvincular(1L, 1L);

        // confirma a desativação sem exclusão física
        assertFalse(resposta.ativo());
        assertFalse(vinculo.getAtivo());

        verify(usuarioRepository).findByIdComBloqueio(1L);
        verify(usuarioUnidadeRepository).save(vinculo);
        verify(usuarioUnidadeRepository, never()).delete(vinculo);
    }

    // teste 11: impede a desvinculação de um vínculo inexistente
    @Test
    void deveRejeitarDesvinculacaoInexistente() {

        Usuario usuario = criarUsuario("ATENDENTE", true);

        when(usuarioRepository.findByIdComBloqueio(1L))
                .thenReturn(Optional.of(usuario));

        when(usuarioUnidadeRepository.findById(
                new UsuarioUnidadeId(1L, 1L)))
                .thenReturn(Optional.empty());

        // verifica a exceção correspondente ao recurso inexistente
        NoSuchElementException excecao = assertThrows(
                NoSuchElementException.class,
                () -> usuarioUnidadeService.desvincular(1L, 1L)
        );

        assertEquals(
                "Vínculo não encontrado.",
                excecao.getMessage()
        );

        verify(usuarioUnidadeRepository, never())
                .save(any(UsuarioUnidade.class));
    }

    // teste 12: lista vínculos ativos e inativos de um usuário
    @Test
    void deveListarVinculosPorUsuario() {

        Usuario usuario = criarUsuario("GERENTE", true);

        Unidade unidade1 = criarUnidade(1L, true);
        Unidade unidade2 = criarUnidade(2L, true);

        UsuarioUnidade vinculoAtivo =
                criarVinculo(usuario, unidade1, true);

        UsuarioUnidade vinculoInativo =
                criarVinculo(usuario, unidade2, false);

        // simula a existência do usuário e seus vínculos
        when(usuarioRepository.existsById(1L))
                .thenReturn(true);

        when(usuarioUnidadeRepository.findByUsuarioId(1L))
                .thenReturn(List.of(vinculoAtivo, vinculoInativo));

        // executa a consulta
        List<UsuarioUnidadeResponseDTO> resposta =
                usuarioUnidadeService.listarPorUsuario(1L);

        // verifica os dois vínculos e suas respectivas situações
        assertEquals(2, resposta.size());

        assertEquals(1L, resposta.get(0).unidadeId());
        assertTrue(resposta.get(0).ativo());

        assertEquals(2L, resposta.get(1).unidadeId());
        assertFalse(resposta.get(1).ativo());

        verify(usuarioUnidadeRepository).findByUsuarioId(1L);
    }
}