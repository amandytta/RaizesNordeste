package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.ConsentimentoAtualResponseDTO;
import br.com.raizesnordeste.raizesnordeste.dto.ConsentimentoRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.ConsentimentoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Consentimento;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.enums.FinalidadeConsentimento;
import br.com.raizesnordeste.raizesnordeste.repository.ConsentimentoRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Map;


// concentra as regras de negócio relacionadas aos consentimentos LGPD
@Service
public class ConsentimentoService {

    // dependências utilizadas pelo serviço
    private final ConsentimentoRepository consentimentoRepository;
    private final UsuarioRepository usuarioRepository;

    // versões vigentes dos termos de consentimento
    private final String versaoTermoMarketing;
    private final String versaoTermoFidelidade;

    // serviço responsável pelo registro dos eventos de auditoria
    private final AuditoriaService auditoriaService;

    // realiza a injeção das dependências e configurações LGPD
    public ConsentimentoService(
            ConsentimentoRepository consentimentoRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoriaService,
            @Value("${app.lgpd.termo.marketing.versao}")
            String versaoTermoMarketing,
            @Value("${app.lgpd.termo.fidelidade.versao}")
            String versaoTermoFidelidade
    ) {
        this.consentimentoRepository = consentimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
        this.versaoTermoMarketing = versaoTermoMarketing;
        this.versaoTermoFidelidade = versaoTermoFidelidade;
    }

    // registra uma nova decisão de consentimento
    // utiliza dados confirmados para evitar leituras desatualizadas
    // durante o processamento de solicitações concorrentes
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ConsentimentoResponseDTO registrar(
            ConsentimentoRequestDTO dto,
            String emailUsuario
    ) {

        // recupera o usuário autenticado
        Usuario usuario = buscarUsuarioAtivo(emailUsuario);

        // bloqueia o usuário para serializar registros concorrentes
        usuario = usuarioRepository
                .findByIdComBloqueio(usuario.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "Usuário sem permissão de acesso."
                ));

        // confirma que o usuário permanece ativo
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {

            throw new AccessDeniedException(
                    "O usuário está inativo."
            );
        }

        // recupera a versão vigente para a finalidade solicitada
        String versaoAtual = obterVersaoAtual(dto.finalidade());

        // remove espaços desnecessários da versão informada
        String versaoInformada = dto.versaoTermo().strip();

        // recupera a última decisão registrada para a finalidade
        Optional<Consentimento> ultimoConsentimento =
                consentimentoRepository
                        .findFirstByUsuarioIdAndFinalidadeOrderByDataRegistroDescIdDesc(
                                usuario.getId(),
                                dto.finalidade()
                        );

        // identifica se o usuário está concedendo o consentimento
        boolean concedido = Boolean.TRUE.equals(dto.concedido());

        // determina a versão que será registrada
        String versaoRegistro;

        if (concedido) {

            // exige a versão vigente para novas concessões
            if (!versaoAtual.equals(versaoInformada)) {

                throw new IllegalArgumentException(
                        "A versão do termo está desatualizada. "
                                + "Versão vigente: "
                                + versaoAtual
                );
            }

            versaoRegistro = versaoAtual;

        } else {

            // associa a revogação à versão da última decisão existente
            // permite revogar mesmo quando o termo foi atualizado
            versaoRegistro = ultimoConsentimento
                    .map(Consentimento::getVersaoTermo)
                    .orElse(versaoAtual);
        }

        // impede a repetição desnecessária da mesma decisão
        if (ultimoConsentimento.isPresent()) {

            Consentimento anterior = ultimoConsentimento.get();

            boolean mesmaDecisao =
                    anterior.getConcedido().equals(dto.concedido());

            boolean mesmaVersao =
                    anterior.getVersaoTermo().equals(versaoRegistro);

            if (mesmaDecisao && mesmaVersao) {

                throw new IllegalArgumentException(
                        "Essa decisão de consentimento já está registrada."
                );
            }
        }

        // cria um novo registro sem sobrescrever decisões anteriores
        Consentimento consentimento = new Consentimento();

        consentimento.setUsuario(usuario);
        consentimento.setFinalidade(dto.finalidade());
        consentimento.setVersaoTermo(versaoRegistro);
        consentimento.setConcedido(dto.concedido());
        consentimento.setOrigem(dto.origem());

        // persiste o registro no histórico de consentimentos
        Consentimento consentimentoSalvo =
                consentimentoRepository.save(consentimento);

        // registra a decisão na auditoria dentro da mesma transação
        auditoriaService.registrar(
                usuario,
                "REGISTRAR",
                "CONSENTIMENTO",
                consentimentoSalvo.getId(),
                Map.of(
                        "finalidade",
                        consentimentoSalvo.getFinalidade().name(),

                        "versaoTermo",
                        consentimentoSalvo.getVersaoTermo(),

                        "concedido",
                        consentimentoSalvo.getConcedido(),

                        "origem",
                        consentimentoSalvo.getOrigem().name()
                )
        );

        // retorna os dados da decisão registrada
        return converterParaResponseDTO(consentimentoSalvo);
    }

    // consulta o histórico de consentimentos do usuário autenticado
    @Transactional(readOnly = true)
    public List<ConsentimentoResponseDTO> listarMeusConsentimentos(
            FinalidadeConsentimento finalidade,
            String emailUsuario
    ) {

        // recupera o usuário responsável pela consulta
        Usuario usuario = buscarUsuarioAtivo(emailUsuario);

        // permite filtrar o histórico por finalidade
        List<Consentimento> consentimentos;

        if (finalidade == null) {

            consentimentos = consentimentoRepository
                    .findByUsuarioIdOrderByDataRegistroDescIdDesc(
                            usuario.getId()
                    );

        } else {

            consentimentos = consentimentoRepository
                    .findByUsuarioIdAndFinalidadeOrderByDataRegistroDescIdDesc(
                            usuario.getId(),
                            finalidade
                    );
        }

        // converte os registros para os DTOs de resposta
        return consentimentos.stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // consulta a situação atual de todas as finalidades
    @Transactional(readOnly = true)
    public List<ConsentimentoAtualResponseDTO> listarMeusConsentimentosAtuais(
            String emailUsuario
    ) {

        // recupera o usuário autenticado
        Usuario usuario = buscarUsuarioAtivo(emailUsuario);

        // consulta a situação de cada finalidade disponível
        return Arrays.stream(FinalidadeConsentimento.values())
                .map(finalidade -> consultarSituacaoAtual(
                        usuario.getId(),
                        finalidade
                ))
                .toList();
    }

    // verifica se o usuário possui consentimento válido
    // poderá ser utilizado internamente por outros módulos
    @Transactional(readOnly = true)
    public boolean possuiConsentimentoValido(
            Long usuarioId,
            FinalidadeConsentimento finalidade
    ) {

        // verifica se o usuário existe e permanece ativo
        Optional<Usuario> usuario =
                usuarioRepository.findById(usuarioId);

        if (usuario.isEmpty()
                || !Boolean.TRUE.equals(usuario.get().getAtivo())) {

            return false;
        }

        // recupera a situação atual do consentimento
        return consultarSituacaoAtual(
                usuarioId,
                finalidade
        ).concedido();
    }

    // determina a situação atual do consentimento por finalidade
    private ConsentimentoAtualResponseDTO consultarSituacaoAtual(
            Long usuarioId,
            FinalidadeConsentimento finalidade
    ) {

        // recupera a versão vigente do termo
        String versaoAtual = obterVersaoAtual(finalidade);

        // recupera a última decisão registrada
        Optional<Consentimento> ultimoConsentimento =
                consentimentoRepository
                        .findFirstByUsuarioIdAndFinalidadeOrderByDataRegistroDescIdDesc(
                                usuarioId,
                                finalidade
                        );

        // ausência de registro não representa autorização
        if (ultimoConsentimento.isEmpty()) {

            return new ConsentimentoAtualResponseDTO(
                    finalidade,
                    false,
                    false,
                    versaoAtual,
                    null,
                    null
            );
        }

        // recupera a decisão mais recente
        Consentimento consentimento = ultimoConsentimento.get();

        // exige uma decisão positiva associada à versão vigente
        boolean consentimentoValido =
                Boolean.TRUE.equals(consentimento.getConcedido())
                        && versaoAtual.equals(
                        consentimento.getVersaoTermo()
                );

        // retorna a situação atual da finalidade
        return new ConsentimentoAtualResponseDTO(
                finalidade,
                true,
                consentimentoValido,
                versaoAtual,
                consentimento.getVersaoTermo(),
                consentimento.getDataRegistro()
        );
    }

    // identifica a versão vigente do termo de cada finalidade
    private String obterVersaoAtual(
            FinalidadeConsentimento finalidade
    ) {

        return switch (finalidade) {

            case MARKETING -> versaoTermoMarketing;

            case FIDELIDADE -> versaoTermoFidelidade;
        };
    }

    // recupera o usuário autenticado e verifica sua situação
    private Usuario buscarUsuarioAtivo(String emailUsuario) {

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

    // converte a entidade para o DTO de resposta
    private ConsentimentoResponseDTO converterParaResponseDTO(
            Consentimento consentimento
    ) {

        return new ConsentimentoResponseDTO(
                consentimento.getId(),
                consentimento.getUsuario().getId(),
                consentimento.getFinalidade(),
                consentimento.getVersaoTermo(),
                consentimento.getConcedido(),
                consentimento.getDataRegistro(),
                consentimento.getOrigem()
        );
    }
}