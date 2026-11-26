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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

// responsável pelo gerenciamento dos vínculos entre funcionários e unidades
@Service
public class UsuarioUnidadeService {

    // repository responsável pelos vínculos
    private final UsuarioUnidadeRepository usuarioUnidadeRepository;

    // repository responsável pelos usuários
    private final UsuarioRepository usuarioRepository;

    // repository responsável pelas unidades
    private final UnidadeRepository unidadeRepository;

    // realiza a injeção das dependências pelo construtor
    public UsuarioUnidadeService(
            UsuarioUnidadeRepository usuarioUnidadeRepository,
            UsuarioRepository usuarioRepository,
            UnidadeRepository unidadeRepository) {

        this.usuarioUnidadeRepository = usuarioUnidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
    }

    // lista todos os vínculos de um determinado usuário
    @Transactional(readOnly = true)
    public List<UsuarioUnidadeResponseDTO> listarPorUsuario(Long usuarioId) {

        // verifica se o usuário informado existe
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new NoSuchElementException("Usuário não encontrado.");
        }

        // consulta os vínculos ativos e inativos do usuário
        return usuarioUnidadeRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // estabelece ou reativa o vínculo de um funcionário com uma unidade
    @Transactional
    public UsuarioUnidadeResponseDTO vincular(
            Long usuarioId,
            UsuarioUnidadeRequestDTO dto) {

        // bloqueia o usuário durante a transação para evitar vínculos concorrentes
        Usuario usuario = usuarioRepository.findByIdComBloqueio(usuarioId)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado."));

        // impede a associação de usuários inativos
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new IllegalArgumentException("O usuário está inativo.");
        }

        // obtém os nomes dos perfis associados ao usuário
        Set<String> perfis = usuario.getPerfis()
                .stream()
                .map(Perfil::getNome)
                .collect(Collectors.toSet());

        // identifica os perfis exclusivos de funcionários
        boolean atendente = perfis.equals(Set.of("ATENDENTE"));
        boolean gerente = perfis.equals(Set.of("GERENTE"));

        // impede vínculos para clientes, administradores e perfis incompatíveis
        if (!atendente && !gerente) {
            throw new IllegalArgumentException(
                    "Somente atendentes e gerentes podem ser vinculados a unidades."
            );
        }

        // busca a unidade informada na requisição
        Unidade unidade = unidadeRepository.findById(dto.unidadeId())
                .orElseThrow(() ->
                        new NoSuchElementException("Unidade não encontrada."));

        // impede a associação com unidades inativas
        if (!Boolean.TRUE.equals(unidade.getAtiva())) {
            throw new IllegalArgumentException("A unidade está inativa.");
        }

        // monta a chave primária composta do vínculo
        UsuarioUnidadeId id = new UsuarioUnidadeId(
                usuarioId,
                dto.unidadeId()
        );

        // consulta um possível vínculo já existente
        UsuarioUnidade vinculo = usuarioUnidadeRepository.findById(id)
                .orElse(null);

        // impede a duplicação de um vínculo que já está ativo
        if (vinculo != null && Boolean.TRUE.equals(vinculo.getAtivo())) {
            throw new IllegalArgumentException(
                    "O usuário já possui vínculo ativo com esta unidade."
            );
        }

        // garante que um atendente possua no máximo uma unidade ativa
        if (atendente
                && usuarioUnidadeRepository.countByUsuarioIdAndAtivoTrue(usuarioId) > 0) {

            throw new IllegalArgumentException(
                    "O atendente já possui vínculo ativo com outra unidade."
            );
        }

        // cria um vínculo caso ainda não exista
        if (vinculo == null) {
            vinculo = new UsuarioUnidade(usuario, unidade);
        }

        // ativa um novo vínculo ou reativa um vínculo existente
        vinculo.setAtivo(true);

        // persiste o vínculo no banco de dados
        UsuarioUnidade vinculoSalvo =
                usuarioUnidadeRepository.save(vinculo);

        // retorna os dados do vínculo persistido
        return converterParaResponseDTO(vinculoSalvo);
    }

    // desativa um vínculo sem excluir seu registro do banco de dados
    @Transactional
    public UsuarioUnidadeResponseDTO desvincular(
            Long usuarioId,
            Long unidadeId) {

        // utiliza o mesmo bloqueio empregado na operação de vinculação
        usuarioRepository.findByIdComBloqueio(usuarioId)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado."));

        // monta a chave composta do vínculo
        UsuarioUnidadeId id = new UsuarioUnidadeId(
                usuarioId,
                unidadeId
        );

        // busca o vínculo existente
        UsuarioUnidade vinculo = usuarioUnidadeRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Vínculo não encontrado."));

        // impede a desativação de um vínculo que já está inativo
        if (!Boolean.TRUE.equals(vinculo.getAtivo())) {
            throw new IllegalArgumentException(
                    "O vínculo já está inativo."
            );
        }

        // desativa o vínculo preservando seu registro original
        vinculo.setAtivo(false);

        // persiste a alteração realizada
        UsuarioUnidade vinculoSalvo =
                usuarioUnidadeRepository.save(vinculo);

        // retorna os dados atualizados
        return converterParaResponseDTO(vinculoSalvo);
    }

    // converte a entidade associativa em um DTO de resposta
    private UsuarioUnidadeResponseDTO converterParaResponseDTO(
            UsuarioUnidade vinculo) {

        // retorna exclusivamente os dados necessários à resposta da API
        return new UsuarioUnidadeResponseDTO(
                vinculo.getUsuario().getId(),
                vinculo.getUsuario().getNome(),
                vinculo.getUnidade().getId(),
                vinculo.getUnidade().getNome(),
                vinculo.getAtivo(),
                vinculo.getDataVinculo()
        );
    }
}