package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.entity.Perfil;
import br.com.raizesnordeste.raizesnordeste.entity.Unidade;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.repository.UnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioUnidadeRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

// centraliza as regras de autorização das operações internas da aplicação
@Service
public class PermissaoUnidadeService {

    // perfis autorizados a consultar informações internas das unidades
    private static final Set<String> PERFIS_CONSULTA =
            Set.of("ADMIN", "GERENTE", "ATENDENTE");

    // perfis autorizados a realizar movimentações manuais de estoque
    private static final Set<String> PERFIS_MOVIMENTACAO =
            Set.of("ADMIN", "GERENTE");

    // perfis autorizados a realizar operações administrativas
    private static final Set<String> PERFIS_ADMINISTRATIVOS =
            Set.of("ADMIN");

    // repository responsável pela consulta dos usuários
    private final UsuarioRepository usuarioRepository;

    // repository responsável pela consulta das unidades
    private final UnidadeRepository unidadeRepository;

    // repository responsável pela consulta dos vínculos
    private final UsuarioUnidadeRepository usuarioUnidadeRepository;

    // realiza a injeção das dependências pelo construtor
    public PermissaoUnidadeService(
            UsuarioRepository usuarioRepository,
            UnidadeRepository unidadeRepository,
            UsuarioUnidadeRepository usuarioUnidadeRepository) {

        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.usuarioUnidadeRepository = usuarioUnidadeRepository;
    }

    // valida se o usuário possui autorização para consultar uma unidade
    @Transactional(readOnly = true)
    public void validarAcesso(String emailUsuario, Long unidadeId) {

        // recupera o usuário e verifica se ele permanece ativo
        Usuario usuario = buscarUsuarioAtivo(emailUsuario);

        // valida o perfil atual do usuário
        String perfil = validarPerfil(
                usuario,
                PERFIS_CONSULTA,
                "O usuário não possui permissão para operar em unidades."
        );

        // verifica a autorização e a situação da unidade
        validarAcessoUnidade(usuario, perfil, unidadeId);
    }

    // valida operações que exigem exclusivamente o perfil ADMIN
    @Transactional(readOnly = true)
    public void validarAcessoAdministrativo(String emailUsuario) {

        // consulta a situação atual do usuário no banco
        Usuario usuario = buscarUsuarioAtivo(emailUsuario);

        // impede operações administrativas por outros perfis
        validarPerfil(
                usuario,
                PERFIS_ADMINISTRATIVOS,
                "O usuário não possui permissão administrativa."
        );
    }

    // valida operações manuais de entrada e saída de estoque
    @Transactional(readOnly = true)
    public void validarMovimentacaoEstoque(
            String emailUsuario,
            Long unidadeId) {

        // recupera o usuário responsável pela operação
        Usuario usuario = buscarUsuarioAtivo(emailUsuario);

        // permite movimentações somente para administradores e gerentes
        String perfil = validarPerfil(
                usuario,
                PERFIS_MOVIMENTACAO,
                "O usuário não possui permissão para movimentar estoque."
        );

        // verifica se o usuário pode operar na unidade informada
        validarAcessoUnidade(usuario, perfil, unidadeId);
    }

    // recupera o usuário autenticado e verifica sua situação atual
    private Usuario buscarUsuarioAtivo(String emailUsuario) {

        // busca o usuário no banco de dados
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new AccessDeniedException(
                        "Usuário sem permissão de acesso."
                ));

        // impede operações realizadas por usuários inativos
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {

            throw new AccessDeniedException(
                    "O usuário está inativo."
            );
        }

        return usuario;
    }

    // verifica se o usuário possui exatamente um perfil autorizado
    private String validarPerfil(
            Usuario usuario,
            Set<String> perfisPermitidos,
            String mensagemErro) {

        // consulta os perfis atuais do usuário
        Set<String> perfis = usuario.getPerfis()
                .stream()
                .map(Perfil::getNome)
                .collect(Collectors.toSet());

        // impede usuários sem perfil ou com múltiplos perfis
        if (perfis.size() != 1) {

            throw new AccessDeniedException(mensagemErro);
        }

        // recupera o único perfil associado ao usuário
        String perfil = perfis.iterator().next();

        // verifica se o perfil está autorizado para a operação
        if (!perfisPermitidos.contains(perfil)) {

            throw new AccessDeniedException(mensagemErro);
        }

        return perfil;
    }

    // centraliza a validação dos vínculos e da situação das unidades
    private void validarAcessoUnidade(
            Usuario usuario,
            String perfil,
            Long unidadeId) {

        // administradores possuem acesso global às unidades ativas
        boolean administrador = "ADMIN".equals(perfil);

        // funcionários precisam possuir um vínculo ativo com a unidade
        if (!administrador
                && !usuarioUnidadeRepository
                .existsByUsuarioIdAndUnidadeIdAndAtivoTrue(
                        usuario.getId(),
                        unidadeId
                )) {

            throw new AccessDeniedException(
                    "O usuário não possui vínculo ativo com esta unidade."
            );
        }

        // busca a unidade após verificar a autorização do usuário
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Unidade não encontrada."
                        )
                );

        // impede operações internas em unidades inativas
        if (!Boolean.TRUE.equals(unidade.getAtiva())) {

            throw new AccessDeniedException(
                    "A unidade está inativa."
            );
        }
    }
}