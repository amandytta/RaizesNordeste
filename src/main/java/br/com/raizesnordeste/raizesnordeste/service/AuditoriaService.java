package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.AuditoriaResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Auditoria;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.repository.AuditoriaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

// centraliza o registro e a consulta dos eventos de auditoria
@Service
public class AuditoriaService {

    // define o tamanho máximo permitido para cada página
    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    // dependências utilizadas pelo serviço
    private final AuditoriaRepository auditoriaRepository;
    private final PermissaoUnidadeService permissaoUnidadeService;

    // realiza a injeção das dependências pelo construtor
    public AuditoriaService(
            AuditoriaRepository auditoriaRepository,
            PermissaoUnidadeService permissaoUnidadeService
    ) {
        this.auditoriaRepository = auditoriaRepository;
        this.permissaoUnidadeService = permissaoUnidadeService;
    }

    // registra um evento dentro da transação da operação original
    // exige que exista uma transação ativa
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(
            Usuario usuario,
            String acao,
            String entidade,
            Long registroId,
            Map<String, Object> detalhes
    ) {

        // normaliza e valida os identificadores do evento
        String acaoNormalizada = normalizarIdentificador(acao);
        String entidadeNormalizada = normalizarIdentificador(entidade);

        // cria o registro de auditoria
        Auditoria auditoria = new Auditoria();

        auditoria.setUsuario(usuario);
        auditoria.setAcao(acaoNormalizada);
        auditoria.setEntidade(entidadeNormalizada);
        auditoria.setRegistroId(registroId);

        // utiliza somente os detalhes selecionados pelo serviço responsável
        // nunca devem ser enviados dados sensíveis ou credenciais
        auditoria.setDetalhes(
                detalhes == null ? null : Map.copyOf(detalhes)
        );

        // persiste o evento na mesma transação da operação original
        auditoriaRepository.save(auditoria);
    }

    // consulta os eventos de auditoria com paginação e filtros opcionais
    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> listar(
            String entidade,
            String acao,
            Long usuarioId,
            Long registroId,
            int pagina,
            int tamanho,
            String emailUsuario
    ) {

        // restringe a consulta aos administradores autorizados
        permissaoUnidadeService.validarAcessoAdministrativo(
                emailUsuario
        );

        // valida os parâmetros de paginação
        if (pagina < 0) {
            throw new IllegalArgumentException(
                    "A página não pode ser negativa."
            );
        }

        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException(
                    "O tamanho da página deve estar entre 1 e 100."
            );
        }

        // valida os identificadores utilizados como filtros
        if (usuarioId != null && usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "O identificador do usuário deve ser positivo."
            );
        }

        if (registroId != null && registroId <= 0) {
            throw new IllegalArgumentException(
                    "O identificador do registro deve ser positivo."
            );
        }

        // normaliza os filtros textuais quando informados
        String entidadeNormalizada = entidade == null
                ? null
                : normalizarIdentificador(entidade);

        String acaoNormalizada = acao == null
                ? null
                : normalizarIdentificador(acao);

        // exige a entidade para identificar corretamente o registro
        if (registroId != null && entidadeNormalizada == null) {
            throw new IllegalArgumentException(
                    "Informe a entidade para filtrar pelo registro."
            );
        }

        // evita combinações de filtros não implementadas
        if ((acaoNormalizada != null
                && (entidadeNormalizada != null || usuarioId != null))
                || (usuarioId != null && entidadeNormalizada != null)) {

            throw new IllegalArgumentException(
                    "A combinação de filtros informada não é permitida."
            );
        }

        // cria a configuração de paginação
        Pageable pageable = PageRequest.of(pagina, tamanho);

        Page<Auditoria> eventos;

        // consulta o histórico de um registro específico
        if (registroId != null) {

            eventos = auditoriaRepository
                    .findByEntidadeAndRegistroIdOrderByDataEventoDescIdDesc(
                            entidadeNormalizada,
                            registroId,
                            pageable
                    );

            // consulta os eventos de determinada entidade
        } else if (entidadeNormalizada != null) {

            eventos = auditoriaRepository
                    .findByEntidadeOrderByDataEventoDescIdDesc(
                            entidadeNormalizada,
                            pageable
                    );

            // consulta os eventos de determinada ação
        } else if (acaoNormalizada != null) {

            eventos = auditoriaRepository
                    .findByAcaoOrderByDataEventoDescIdDesc(
                            acaoNormalizada,
                            pageable
                    );

            // consulta os eventos realizados por determinado usuário
        } else if (usuarioId != null) {

            eventos = auditoriaRepository
                    .findByUsuarioIdOrderByDataEventoDescIdDesc(
                            usuarioId,
                            pageable
                    );

            // consulta todos os eventos de auditoria
        } else {

            eventos = auditoriaRepository
                    .findAllByOrderByDataEventoDescIdDesc(pageable);
        }

        // converte as entidades sem perder os dados da paginação
        return eventos.map(this::converterParaResponseDTO);
    }

    // consulta um evento de auditoria pelo identificador
    @Transactional(readOnly = true)
    public AuditoriaResponseDTO buscarPorId(
            Long id,
            String emailUsuario
    ) {

        // verifica a permissão administrativa
        permissaoUnidadeService.validarAcessoAdministrativo(
                emailUsuario
        );

        // busca o evento ou informa que ele não existe
        Auditoria auditoria = auditoriaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Registro de auditoria não encontrado."
                ));

        return converterParaResponseDTO(auditoria);
    }

    // padroniza os identificadores de ações e entidades
    private String normalizarIdentificador(String valor) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "O identificador de auditoria é obrigatório."
            );
        }

        String normalizado = valor.strip()
                .toUpperCase(Locale.ROOT);

        // permite somente identificadores padronizados
        if (!normalizado.matches("[A-Z][A-Z0-9_]{0,79}")) {
            throw new IllegalArgumentException(
                    "Identificador de auditoria inválido."
            );
        }

        return normalizado;
    }

    // converte a entidade para o objeto de resposta da API
    private AuditoriaResponseDTO converterParaResponseDTO(
            Auditoria auditoria
    ) {

        // permite eventos sem um usuário responsável identificado
        Long usuarioId = auditoria.getUsuario() == null
                ? null
                : auditoria.getUsuario().getId();

        return new AuditoriaResponseDTO(
                auditoria.getId(),
                usuarioId,
                auditoria.getAcao(),
                auditoria.getEntidade(),
                auditoria.getRegistroId(),
                auditoria.getDetalhes(),
                auditoria.getDataEvento()
        );
    }
}