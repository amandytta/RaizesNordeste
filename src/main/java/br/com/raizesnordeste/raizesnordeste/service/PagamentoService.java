package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.GatewayPagamentoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.dto.PagamentoRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.PagamentoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.*;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPagamento;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPedido;
import br.com.raizesnordeste.raizesnordeste.enums.TipoMovimentacaoEstoque;
import br.com.raizesnordeste.raizesnordeste.gateway.GatewayPagamentoMock;
import br.com.raizesnordeste.raizesnordeste.repository.MovimentacaoEstoqueRepository;
import br.com.raizesnordeste.raizesnordeste.repository.PagamentoRepository;
import br.com.raizesnordeste.raizesnordeste.repository.PedidoRepository;
import br.com.raizesnordeste.raizesnordeste.repository.ProdutoUnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

// concentra as regras de negócio relacionadas aos pagamentos
@Service
public class PagamentoService {

    // define os perfis autorizados a utilizar o módulo
    private static final Set<String> PERFIS_PERMITIDOS =
            Set.of("ADMIN", "GERENTE", "ATENDENTE", "CLIENTE");

    // dependências utilizadas no processamento dos pagamentos
    // serviço responsável pela auditoria transacional
    private final AuditoriaService auditoriaService;

    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final ProdutoUnidadeRepository produtoUnidadeRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermissaoUnidadeService permissaoUnidadeService;
    private final GatewayPagamentoMock gatewayPagamentoMock;

    // realiza a injeção das dependências pelo construtor
    public PagamentoService(
            PagamentoRepository pagamentoRepository,
            PedidoRepository pedidoRepository,
            ProdutoUnidadeRepository produtoUnidadeRepository,
            MovimentacaoEstoqueRepository movimentacaoEstoqueRepository,
            UsuarioRepository usuarioRepository,
            PermissaoUnidadeService permissaoUnidadeService,
            GatewayPagamentoMock gatewayPagamentoMock,
            AuditoriaService auditoriaService
    ) {
        this.auditoriaService = auditoriaService;
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.produtoUnidadeRepository = produtoUnidadeRepository;
        this.movimentacaoEstoqueRepository = movimentacaoEstoqueRepository;
        this.usuarioRepository = usuarioRepository;
        this.permissaoUnidadeService = permissaoUnidadeService;
        this.gatewayPagamentoMock = gatewayPagamentoMock;
    }

    // processa uma tentativa de pagamento em uma única transação
    @Transactional
    public PagamentoResponseDTO processar(
            Long pedidoId,
            PagamentoRequestDTO dto,
            String emailUsuario
    ) {

        // recupera o pedido com bloqueio exclusivo para atualização
        Pedido pedido = pedidoRepository.findByIdComBloqueio(pedidoId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Pedido não encontrado."
                ));

        // recupera o usuário e valida sua autorização sobre o pedido
        Usuario usuario = validarAcessoAoPedido(
                pedido,
                emailUsuario
        );

        // impede pagamentos em unidades inativas
        if (!Boolean.TRUE.equals(pedido.getUnidade().getAtiva())) {
            throw new IllegalArgumentException(
                    "Não é possível pagar pedidos de uma unidade inativa."
            );
        }

        // impede pagamentos de pedidos que não aguardam pagamento
        if (pedido.getStatus()
                != StatusPedido.AGUARDANDO_PAGAMENTO) {

            throw new IllegalArgumentException(
                    "O pedido não está aguardando pagamento."
            );
        }

        // verifica se já existe uma transação aprovada
        if (pagamentoRepository.existsByPedidoIdAndStatus(
                pedidoId,
                StatusPagamento.APROVADO
        )) {

            throw new IllegalArgumentException(
                    "O pedido já possui um pagamento aprovado."
            );
        }

        // garante que o pedido possua um valor válido
        if (pedido.getValorTotal() == null
                || pedido.getValorTotal().signum() <= 0) {

            throw new IllegalArgumentException(
                    "O valor do pedido deve ser maior que zero."
            );
        }

        // impede o pagamento de pedidos sem itens
        if (pedido.getItens().isEmpty()) {
            throw new IllegalArgumentException(
                    "Não é possível pagar um pedido sem itens."
            );
        }

        // registra o momento de criação da tentativa
        LocalDateTime dataInicio = LocalDateTime.now();

        // solicita o processamento ao gateway exclusivamente simulado
        GatewayPagamentoResponseDTO resultado =
                gatewayPagamentoMock.processar(
                        dto.metodo(),
                        pedido.getValorTotal(),
                        dto.resultadoSimulado()
                );

        // realiza a baixa de estoque somente após aprovação simulada
        if (resultado.status() == StatusPagamento.APROVADO) {

            // valida novamente o estoque e registra as saídas
            baixarEstoque(pedido, usuario);

            // atualiza o pedido para iniciar o atendimento
            pedido.setStatus(StatusPedido.RECEBIDO);
        }

        // cria o registro da tentativa de pagamento
        Pagamento pagamento = new Pagamento();

        pagamento.setPedido(pedido);
        pagamento.setDataCriacao(dataInicio);
        pagamento.setMetodo(dto.metodo());
        pagamento.setStatus(resultado.status());
        pagamento.setValor(pedido.getValorTotal());
        pagamento.setCodigoTransacao(
                resultado.codigoTransacao()
        );
        pagamento.setMensagemRetorno(
                resultado.mensagemRetorno()
        );
        pagamento.setDataProcessamento(
                resultado.dataProcessamento()
        );

        // persiste o resultado da tentativa de pagamento
        Pagamento pagamentoSalvo =
                pagamentoRepository.save(pagamento);


        // registra o evento de auditoria na mesma transação
        auditoriaService.registrar(
                usuario, resultado.status() == StatusPagamento.APROVADO ? "APROVAR" : "RECUSAR", "PAGAMENTO", pagamentoSalvo.getId(),
                Map.of("pedidoId", pedidoId, "metodo", dto.metodo().name(), "valor", pagamentoSalvo.getValor(), "statusPedido", pedido.getStatus().name())
        );

        // retorna os dados da transação registrada
        return converterParaResponseDTO(pagamentoSalvo);
    }

    // realiza a baixa de estoque dos itens de um pedido aprovado
    private void baixarEstoque(
            Pedido pedido,
            Usuario usuario
    ) {

        // ordena os itens pelo identificador do estoque
        // reduz o risco de deadlocks entre pedidos concorrentes
        List<ItemPedido> itensOrdenados = pedido.getItens().stream()
                .sorted(Comparator.comparing(
                        item -> item.getProdutoUnidade().getId()
                ))
                .toList();

        // percorre os itens e bloqueia os respectivos estoques
        for (ItemPedido item : itensOrdenados) {

            Long produtoUnidadeId =
                    item.getProdutoUnidade().getId();

            // recupera o estoque com bloqueio exclusivo
            ProdutoUnidade produtoUnidade =
                    produtoUnidadeRepository
                            .findByIdComBloqueio(produtoUnidadeId)
                            .orElseThrow(() -> new NoSuchElementException(
                                    "Produto não encontrado na unidade."
                            ));

            // confirma que o produto pertence à unidade do pedido
            if (!produtoUnidade.getUnidade().getId()
                    .equals(pedido.getUnidade().getId())) {

                throw new IllegalArgumentException(
                        "O produto não pertence à unidade do pedido."
                );
            }

            // verifica se o produto permanece ativo
            if (!Boolean.TRUE.equals(
                    produtoUnidade.getProduto().getAtivo()
            )) {

                throw new IllegalArgumentException(
                        "O pedido possui um produto inativo."
                );
            }

            // verifica se o produto permanece disponível
            if (!Boolean.TRUE.equals(
                    produtoUnidade.getDisponivel()
            )) {

                throw new IllegalArgumentException(
                        "O pedido possui um produto indisponível."
                );
            }

            // recupera a quantidade solicitada no pedido
            Integer quantidade = item.getQuantidade();

            // protege a operação contra quantidades inválidas
            if (quantidade == null || quantidade <= 0) {

                throw new IllegalArgumentException(
                        "O pedido possui uma quantidade inválida."
                );
            }

            // recupera o saldo atual do estoque
            int estoqueAnterior = produtoUnidade.getEstoque();

            // impede vendas com estoque insuficiente
            if (estoqueAnterior < quantidade) {

                throw new IllegalArgumentException(
                        "Estoque insuficiente para o produto: "
                                + item.getNomeProduto()
                );
            }

            // calcula o saldo após a saída
            int estoquePosterior =
                    estoqueAnterior - quantidade;

            // atualiza o saldo do produto na unidade
            produtoUnidade.setEstoque(estoquePosterior);

            // registra a movimentação automática de saída
            MovimentacaoEstoque movimentacao =
                    new MovimentacaoEstoque();

            movimentacao.setProdutoUnidade(produtoUnidade);
            movimentacao.setPedidoId(pedido.getId());
            movimentacao.setUsuario(usuario);
            movimentacao.setTipo(TipoMovimentacaoEstoque.SAIDA);
            movimentacao.setQuantidade(quantidade);
            movimentacao.setEstoqueAnterior(estoqueAnterior);
            movimentacao.setEstoquePosterior(estoquePosterior);
            movimentacao.setMotivo(
                    "Pagamento aprovado do pedido #" + pedido.getId()
            );

            // persiste o histórico da movimentação de estoque
            movimentacaoEstoqueRepository.save(movimentacao);
        }
    }

    // consulta o histórico de pagamentos de um pedido autorizado
    @Transactional(readOnly = true)
    public List<PagamentoResponseDTO> listarPorPedido(
            Long pedidoId,
            String emailUsuario
    ) {

        // recupera o pedido solicitado
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Pedido não encontrado."
                ));

        // verifica se o usuário possui acesso ao pedido
        validarAcessoAoPedido(pedido, emailUsuario);

        // recupera e converte as tentativas de pagamento
        return pagamentoRepository
                .findByPedidoIdOrderByDataCriacaoDescIdDesc(pedidoId)
                .stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // consulta um pagamento específico dentro de um pedido
    @Transactional(readOnly = true)
    public PagamentoResponseDTO buscarPorId(
            Long pedidoId,
            Long pagamentoId,
            String emailUsuario
    ) {

        // recupera o pedido solicitado
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Pedido não encontrado."
                ));

        // valida a autorização antes de consultar a transação
        validarAcessoAoPedido(pedido, emailUsuario);

        // recupera o pagamento solicitado
        Pagamento pagamento =
                pagamentoRepository.findById(pagamentoId)
                        .orElseThrow(() -> new NoSuchElementException(
                                "Pagamento não encontrado."
                        ));

        // impede a consulta de pagamentos pertencentes a outro pedido
        if (!pagamento.getPedido().getId().equals(pedidoId)) {

            throw new NoSuchElementException(
                    "Pagamento não encontrado."
            );
        }

        return converterParaResponseDTO(pagamento);
    }

    // valida o usuário autenticado e sua autorização sobre o pedido
    private Usuario validarAcessoAoPedido(
            Pedido pedido,
            String emailUsuario
    ) {

        // recupera o usuário autenticado
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

        // recupera os perfis associados ao usuário
        Set<String> perfis = usuario.getPerfis().stream()
                .map(Perfil::getNome)
                .collect(Collectors.toSet());

        // exige exatamente um perfil reconhecido pela aplicação
        if (perfis.size() != 1
                || !PERFIS_PERMITIDOS.containsAll(perfis)) {

            throw new AccessDeniedException(
                    "O usuário não possui um perfil válido."
            );
        }

        String perfil = perfis.iterator().next();

        // clientes podem acessar somente seus próprios pedidos
        if ("CLIENTE".equals(perfil)) {

            if (pedido.getUsuario() == null
                    || !pedido.getUsuario().getId()
                    .equals(usuario.getId())) {

                throw new NoSuchElementException(
                        "Pedido não encontrado."
                );
            }

        } else {

            // funcionários devem possuir autorização sobre a unidade
            permissaoUnidadeService.validarAcesso(
                    emailUsuario,
                    pedido.getUnidade().getId()
            );
        }

        return usuario;
    }

    // converte a entidade Pagamento para o DTO de resposta
    private PagamentoResponseDTO converterParaResponseDTO(
            Pagamento pagamento
    ) {

        return new PagamentoResponseDTO(
                pagamento.getId(),
                pagamento.getPedido().getId(),
                pagamento.getMetodo(),
                pagamento.getStatus(),
                pagamento.getValor(),
                pagamento.getCodigoTransacao(),
                pagamento.getMensagemRetorno(),
                pagamento.getPedido().getStatus(),
                pagamento.getDataCriacao(),
                pagamento.getDataProcessamento()
        );
    }
}