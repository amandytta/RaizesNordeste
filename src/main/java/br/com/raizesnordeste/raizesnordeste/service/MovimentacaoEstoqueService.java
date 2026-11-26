package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.MovimentacaoEstoqueRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.MovimentacaoEstoqueResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.MovimentacaoEstoque;
import br.com.raizesnordeste.raizesnordeste.entity.ProdutoUnidade;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.enums.TipoMovimentacaoEstoque;
import br.com.raizesnordeste.raizesnordeste.repository.MovimentacaoEstoqueRepository;
import br.com.raizesnordeste.raizesnordeste.repository.ProdutoUnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final ProdutoUnidadeRepository produtoUnidadeRepository;
    private final UsuarioRepository usuarioRepository;

    // serviço responsável pelas regras de autorização por unidade
    private final PermissaoUnidadeService permissaoUnidadeService;

    // injeta os repositórios e o serviço de permissões
    public MovimentacaoEstoqueService(
            MovimentacaoEstoqueRepository movimentacaoEstoqueRepository,
            ProdutoUnidadeRepository produtoUnidadeRepository,
            UsuarioRepository usuarioRepository,
            PermissaoUnidadeService permissaoUnidadeService) {

        this.movimentacaoEstoqueRepository = movimentacaoEstoqueRepository;
        this.produtoUnidadeRepository = produtoUnidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.permissaoUnidadeService = permissaoUnidadeService;
    }

    // lista o histórico global, exclusivamente para administradores
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarTodas(
            String emailUsuario) {

        permissaoUnidadeService.validarAcessoAdministrativo(emailUsuario);

        return movimentacaoEstoqueRepository
                .findAllByOrderByDataMovimentacaoDesc()
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::fromEntity)
                .toList();
    }

    // busca uma movimentação individual respeitando o acesso à unidade
    @Transactional(readOnly = true)
    public MovimentacaoEstoqueResponseDTO buscarPorId(
            Long id,
            String emailUsuario) {

        MovimentacaoEstoque movimentacao =
                movimentacaoEstoqueRepository.findById(id)
                        .orElseThrow(() -> new NoSuchElementException(
                                "Movimentação de estoque não encontrada: " + id
                        ));

        // identifica a unidade real à qual a movimentação pertence
        Long unidadeId = movimentacao
                .getProdutoUnidade()
                .getUnidade()
                .getId();

        // valida a autorização antes de retornar os dados
        permissaoUnidadeService.validarAcesso(emailUsuario, unidadeId);

        return MovimentacaoEstoqueResponseDTO.fromEntity(movimentacao);
    }

    // lista o histórico de um produto em uma unidade específica
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorProdutoUnidade(
            Long produtoUnidadeId,
            String emailUsuario) {

        // recupera a associação para identificar sua unidade
        ProdutoUnidade produtoUnidade =
                produtoUnidadeRepository.findById(produtoUnidadeId)
                        .orElseThrow(() -> new NoSuchElementException(
                                "Associação de produto e unidade não encontrada: "
                                        + produtoUnidadeId
                        ));

        // impede consultas ao estoque de unidades não autorizadas
        permissaoUnidadeService.validarAcesso(
                emailUsuario,
                produtoUnidade.getUnidade().getId()
        );

        return movimentacaoEstoqueRepository
                .findByProdutoUnidadeIdOrderByDataMovimentacaoDesc(
                        produtoUnidadeId
                )
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::fromEntity)
                .toList();
    }

    // lista as movimentações de uma unidade autorizada
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorUnidade(
            Long unidadeId,
            String emailUsuario) {

        permissaoUnidadeService.validarAcesso(emailUsuario, unidadeId);

        return movimentacaoEstoqueRepository
                .findByProdutoUnidadeUnidadeIdOrderByDataMovimentacaoDesc(
                        unidadeId
                )
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::fromEntity)
                .toList();
    }

    // o filtro global por tipo é exclusivo de administradores
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorTipo(
            TipoMovimentacaoEstoque tipo,
            String emailUsuario) {

        permissaoUnidadeService.validarAcessoAdministrativo(emailUsuario);

        return movimentacaoEstoqueRepository
                .findByTipoOrderByDataMovimentacaoDesc(tipo)
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::fromEntity)
                .toList();
    }

    // registra a movimentação e atualiza o estoque na mesma transação
    @Transactional
    public MovimentacaoEstoqueResponseDTO registrar(
            MovimentacaoEstoqueRequestDTO dto,
            String emailUsuario) {

        // recupera o usuário responsável pelo registro
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new NoSuchElementException(
                        "Usuário responsável não encontrado."
                ));

        // impede movimentações com quantidades inválidas
        if (dto.quantidade() == null || dto.quantidade() <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade deve ser maior que zero."
            );
        }

        // o ajuste manual de estoque ainda não faz parte do MVP
        if (dto.tipo() == TipoMovimentacaoEstoque.AJUSTE) {
            throw new IllegalArgumentException(
                    "Movimentações do tipo AJUSTE ainda não estão disponíveis."
            );
        }

        // bloqueia o registro de estoque durante a transação, evitando alterações concorrentes inconsistentes
        ProdutoUnidade produtoUnidade =
                produtoUnidadeRepository
                        .findByIdComBloqueio(dto.produtoUnidadeId())
                        .orElseThrow(() -> new NoSuchElementException(
                                "Associação de produto e unidade não encontrada: "
                                        + dto.produtoUnidadeId()
                        ));

        // somente administradores e gerentes autorizados podem registrar entradas e saídas de estoque
        permissaoUnidadeService.validarMovimentacaoEstoque(
                emailUsuario,
                produtoUnidade.getUnidade().getId()
        );

        // preserva a regra de negócio que impede alterações no estoque de unidades inativas
        if (!Boolean.TRUE.equals(
                produtoUnidade.getUnidade().getAtiva())) {

            throw new IllegalArgumentException(
                    "Não é possível movimentar o estoque de uma unidade inativa."
            );
        }

        // armazena o saldo anterior para compor o histórico
        int estoqueAnterior = produtoUnidade.getEstoque();
        int estoquePosterior;

        switch (dto.tipo()) {

            case ENTRADA -> {

                // soma a quantidade verificando o limite do tipo Integer
                try {
                    estoquePosterior = Math.addExact(
                            estoqueAnterior,
                            dto.quantidade()
                    );
                } catch (ArithmeticException excecao) {
                    throw new IllegalArgumentException(
                            "A movimentação excede o limite máximo de estoque."
                    );
                }
            }

            case SAIDA -> {

                // impede que uma retirada deixe o estoque negativo
                if (dto.quantidade() > estoqueAnterior) {
                    throw new IllegalArgumentException(
                            "Estoque insuficiente para realizar a saída."
                    );
                }

                estoquePosterior = estoqueAnterior - dto.quantidade();
            }

            default -> throw new IllegalArgumentException(
                    "Tipo de movimentação não permitido."
            );
        }

        // atualiza e persiste o novo saldo
        produtoUnidade.setEstoque(estoquePosterior);
        produtoUnidadeRepository.save(produtoUnidade);

        // cria o registro histórico da movimentação
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();

        movimentacao.setProdutoUnidade(produtoUnidade);
        movimentacao.setUsuario(usuario);
        movimentacao.setTipo(dto.tipo());
        movimentacao.setQuantidade(dto.quantidade());
        movimentacao.setEstoqueAnterior(estoqueAnterior);
        movimentacao.setEstoquePosterior(estoquePosterior);
        movimentacao.setMotivo(dto.motivo());

        // persiste o histórico dentro da mesma transação do estoque
        MovimentacaoEstoque movimentacaoSalva =
                movimentacaoEstoqueRepository.save(movimentacao);

        return MovimentacaoEstoqueResponseDTO.fromEntity(movimentacaoSalva);
    }
}