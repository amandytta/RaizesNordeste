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

// camada responsável pelas regras de negócio relacionadas às movimentações de estoque
@Service
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final ProdutoUnidadeRepository produtoUnidadeRepository;
    private final UsuarioRepository usuarioRepository;

    // injeção de dependências através do construtor
    public MovimentacaoEstoqueService(
            MovimentacaoEstoqueRepository movimentacaoEstoqueRepository,
            ProdutoUnidadeRepository produtoUnidadeRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.movimentacaoEstoqueRepository = movimentacaoEstoqueRepository;
        this.produtoUnidadeRepository = produtoUnidadeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // lista todas as movimentações de estoque, ordenadas da mais recente para a mais antiga
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarTodas() {

        return movimentacaoEstoqueRepository
                .findAllByOrderByDataMovimentacaoDesc()
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::fromEntity)
                .toList();
    }

    // busca uma movimentação específica pelo seu identificador
    @Transactional(readOnly = true)
    public MovimentacaoEstoqueResponseDTO buscarPorId(Long id) {

        MovimentacaoEstoque movimentacao = movimentacaoEstoqueRepository
                .findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Movimentação de estoque não encontrada: " + id
                ));

        return MovimentacaoEstoqueResponseDTO.fromEntity(movimentacao);
    }

    // consulta o histórico de movimentações de um produto associado a uma determinada unidade
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorProdutoUnidade(
            Long produtoUnidadeId
    ) {

        // verifica se a associação existe antes de consultar o histórico
        if (!produtoUnidadeRepository.existsById(produtoUnidadeId)) {
            throw new NoSuchElementException(
                    "Associação de produto e unidade não encontrada: "
                            + produtoUnidadeId
            );
        }

        return movimentacaoEstoqueRepository
                .findByProdutoUnidadeIdOrderByDataMovimentacaoDesc(
                        produtoUnidadeId
                )
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::fromEntity)
                .toList();
    }

    // consulta o histórico de movimentações de uma unidade
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorUnidade(
            Long unidadeId
    ) {

        return movimentacaoEstoqueRepository
                .findByProdutoUnidadeUnidadeIdOrderByDataMovimentacaoDesc(
                        unidadeId
                )
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::fromEntity)
                .toList();
    }

    // filtra as movimentações pelo tipo informado
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorTipo(
            TipoMovimentacaoEstoque tipo
    ) {

        return movimentacaoEstoqueRepository
                .findByTipoOrderByDataMovimentacaoDesc(tipo)
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::fromEntity)
                .toList();
    }

    // registra uma movimentação e atualiza o saldo do estoque
    // a anotação @Transactional garante que as alterações sejam realizadas dentro de uma única transação
    // caso ocorra uma exceção não verificada, as alterações serão revertidas automaticamente
    @Transactional
    public MovimentacaoEstoqueResponseDTO registrar(
            MovimentacaoEstoqueRequestDTO dto,
            String emailUsuario
    ) {

        // identifica o usuário responsável pela operação
        // o e-mail deverá ser obtido da autenticação, e não informado diretamente no corpo da requisição
        Usuario usuario = usuarioRepository
                .findByEmail(emailUsuario)
                .orElseThrow(() -> new NoSuchElementException(
                        "Usuário responsável não encontrado."
                ));

        // validação adicional para proteger a camada de serviço
        if (dto.quantidade() == null || dto.quantidade() <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade deve ser maior que zero."
            );
        }

        // o ajuste de estoque será implementado posteriormente, após a definição de suas regras específicas
        if (dto.tipo() == TipoMovimentacaoEstoque.AJUSTE) {
            throw new IllegalArgumentException(
                    "Movimentações do tipo AJUSTE ainda não estão disponíveis."
            );
        }

        // busca o produto da unidade com bloqueio pessimista
        // isso evita alterações concorrentes no mesmo saldo
        ProdutoUnidade produtoUnidade = produtoUnidadeRepository
                .findByIdComBloqueio(dto.produtoUnidadeId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Associação de produto e unidade não encontrada: "
                                + dto.produtoUnidadeId()
                ));

        // impede movimentações em unidades inativas
        if (!Boolean.TRUE.equals(produtoUnidade.getUnidade().getAtiva())) {
            throw new IllegalArgumentException(
                    "Não é possível movimentar o estoque de uma unidade inativa."
            );
        }

        // armazena o saldo existente antes da movimentação
        int estoqueAnterior = produtoUnidade.getEstoque();

        int estoquePosterior;

        // calcula o novo saldo conforme o tipo de movimentação
        switch (dto.tipo()) {

            case ENTRADA -> {

                // Math.addExact protege contra estouro do limite de armazenamento de um Integer
                try {
                    estoquePosterior = Math.addExact(
                            estoqueAnterior,
                            dto.quantidade()
                    );
                } catch (ArithmeticException e) {
                    throw new IllegalArgumentException(
                            "A movimentação excede o limite máximo de estoque."
                    );
                }
            }

            case SAIDA -> {

                // impede que o estoque fique negativo
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

        // atualiza o saldo atual do produto na unidade
        produtoUnidade.setEstoque(estoquePosterior);

        produtoUnidadeRepository.save(produtoUnidade);

        // cria o registro que será armazenado no histórico de movimentações de estoque
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();

        movimentacao.setProdutoUnidade(produtoUnidade);
        movimentacao.setUsuario(usuario);
        movimentacao.setTipo(dto.tipo());
        movimentacao.setQuantidade(dto.quantidade());
        movimentacao.setEstoqueAnterior(estoqueAnterior);
        movimentacao.setEstoquePosterior(estoquePosterior);
        movimentacao.setMotivo(dto.motivo());

        // persiste o histórico da movimentação
        MovimentacaoEstoque movimentacaoSalva =
                movimentacaoEstoqueRepository.save(movimentacao);

        // converte a entidade salva em um DTO de resposta
        return MovimentacaoEstoqueResponseDTO.fromEntity(
                movimentacaoSalva
        );
    }
}