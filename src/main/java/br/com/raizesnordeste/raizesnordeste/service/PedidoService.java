package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.ItemPedidoRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.ItemPedidoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.dto.PedidoRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.PedidoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.*;
import br.com.raizesnordeste.raizesnordeste.enums.CanalPedido;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPedido;
import br.com.raizesnordeste.raizesnordeste.repository.PedidoRepository;
import br.com.raizesnordeste.raizesnordeste.repository.ProdutoUnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

// concentra as regras de negócio relacionadas aos pedidos
@Service
public class PedidoService {

    // representa o maior valor permitido pelas colunas DECIMAL(10,2)
    private static final BigDecimal VALOR_MAXIMO =
            new BigDecimal("99999999.99");

    // define os perfis autorizados a utilizar o módulo de pedidos
    private static final Set<String> PERFIS_PERMITIDOS =
            Set.of("ADMIN", "GERENTE", "ATENDENTE", "CLIENTE");

    // dependências utilizadas pelas operações de pedidos
    private final PedidoRepository pedidoRepository;
    private final ProdutoUnidadeRepository produtoUnidadeRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermissaoUnidadeService permissaoUnidadeService;

    // realiza a injeção das dependências pelo construtor
    public PedidoService(
            PedidoRepository pedidoRepository,
            ProdutoUnidadeRepository produtoUnidadeRepository,
            UnidadeRepository unidadeRepository,
            UsuarioRepository usuarioRepository,
            PermissaoUnidadeService permissaoUnidadeService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.produtoUnidadeRepository = produtoUnidadeRepository;
        this.unidadeRepository = unidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.permissaoUnidadeService = permissaoUnidadeService;
    }

    // cadastra um novo pedido juntamente com seus itens
    @Transactional
    public PedidoResponseDTO cadastrar(
            PedidoRequestDTO dto,
            String emailUsuario
    ) {

        // recupera o usuário autenticado e valida seu perfil
        Usuario usuario = buscarUsuarioAtivo(emailUsuario);
        String perfil = identificarPerfil(usuario);

        // restringe funcionários às unidades autorizadas
        if (!"CLIENTE".equals(perfil)) {
            permissaoUnidadeService.validarAcesso(
                    emailUsuario,
                    dto.unidadeId()
            );
        }

        // recupera a unidade responsável pelo pedido
        Unidade unidade = unidadeRepository.findById(dto.unidadeId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Unidade não encontrada."
                ));

        // impede a realização de pedidos em unidades inativas
        if (!Boolean.TRUE.equals(unidade.getAtiva())) {
            throw new IllegalArgumentException(
                    "Não é possível realizar pedidos em uma unidade inativa."
            );
        }

        // garante que o pedido possua pelo menos um item
        if (dto.itens() == null || dto.itens().isEmpty()) {
            throw new IllegalArgumentException(
                    "O pedido deve possuir pelo menos um item."
            );
        }

        // inicializa o pedido com os dados controlados pelo backend
        Pedido pedido = new Pedido();
        pedido.setUnidade(unidade);
        pedido.setUsuario(usuario);
        pedido.setCanalPedido(dto.canalPedido());
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);

        // inicializa o valor total do pedido
        BigDecimal valorTotal = BigDecimal.ZERO;

        // controla os produtos já adicionados ao pedido
        Set<Long> produtosAdicionados = new HashSet<>();

        // percorre e valida todos os itens solicitados
        for (ItemPedidoRequestDTO itemDTO : dto.itens()) {

            // impede a repetição da mesma associação de produto e unidade
            if (!produtosAdicionados.add(itemDTO.produtoUnidadeId())) {
                throw new IllegalArgumentException(
                        "O pedido não pode possuir produtos duplicados."
                );
            }

            // recupera a associação entre produto e unidade
            ProdutoUnidade produtoUnidade =
                    produtoUnidadeRepository
                            .findById(itemDTO.produtoUnidadeId())
                            .orElseThrow(() -> new NoSuchElementException(
                                    "Produto não encontrado na unidade."
                            ));

            // impede a inclusão de produtos pertencentes a outra unidade
            if (!produtoUnidade.getUnidade().getId()
                    .equals(unidade.getId())) {
                throw new IllegalArgumentException(
                        "Todos os produtos devem pertencer à unidade do pedido."
                );
            }

            // verifica se o produto está ativo
            if (!Boolean.TRUE.equals(
                    produtoUnidade.getProduto().getAtivo()
            )) {
                throw new IllegalArgumentException(
                        "O pedido possui um produto inativo."
                );
            }

            // verifica a disponibilidade comercial do produto
            if (!Boolean.TRUE.equals(
                    produtoUnidade.getDisponivel()
            )) {
                throw new IllegalArgumentException(
                        "O pedido possui um produto indisponível."
                );
            }

            // garante que a quantidade solicitada seja válida
            if (itemDTO.quantidade() == null
                    || itemDTO.quantidade() <= 0) {
                throw new IllegalArgumentException(
                        "A quantidade deve ser maior que zero."
                );
            }

            // verifica o saldo disponível sem realizar a baixa
            if (produtoUnidade.getEstoque() < itemDTO.quantidade()) {
                throw new IllegalArgumentException(
                        "Estoque insuficiente para o produto: "
                                + produtoUnidade.getProduto().getNome()
                );
            }

            // recupera o preço cadastrado no banco de dados
            BigDecimal precoUnitario = produtoUnidade.getPreco();

            // calcula o subtotal do item
            BigDecimal subtotal = precoUnitario.multiply(
                    BigDecimal.valueOf(itemDTO.quantidade())
            );

            // impede valores superiores à capacidade do banco
            validarLimiteMonetario(subtotal);

            // cria o item preservando os dados comerciais da compra
            ItemPedido item = new ItemPedido();
            item.setProdutoUnidade(produtoUnidade);
            item.setNomeProduto(
                    produtoUnidade.getProduto().getNome()
            );
            item.setQuantidade(itemDTO.quantidade());
            item.setPrecoUnitario(precoUnitario);
            item.setSubtotal(subtotal);

            // associa o item ao pedido
            pedido.adicionarItem(item);

            // acumula o subtotal no valor total
            valorTotal = valorTotal.add(subtotal);

            // valida o limite monetário do pedido
            validarLimiteMonetario(valorTotal);
        }

        // define o valor total calculado pelo backend
        pedido.setValorTotal(valorTotal);

        // persiste o pedido e seus itens na mesma transação
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // retorna os dados do pedido cadastrado
        return converterParaResponseDTO(pedidoSalvo);
    }

    // lista todos os pedidos da rede com filtro opcional por canal
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarTodos(
            CanalPedido canalPedido,
            String emailUsuario
    ) {

        // restringe o histórico global aos administradores
        permissaoUnidadeService.validarAcessoAdministrativo(
                emailUsuario
        );

        // realiza a consulta de acordo com o filtro informado
        List<Pedido> pedidos = canalPedido == null
                ? pedidoRepository.findAllByOrderByDataCriacaoDesc()
                : pedidoRepository
                .findByCanalPedidoOrderByDataCriacaoDesc(
                        canalPedido
                );

        // converte os pedidos para o formato de resposta da API
        return pedidos.stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // lista os pedidos de uma unidade com filtro opcional por canal
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPorUnidade(
            Long unidadeId,
            CanalPedido canalPedido,
            String emailUsuario
    ) {

        // verifica se o usuário possui acesso à unidade solicitada
        permissaoUnidadeService.validarAcesso(
                emailUsuario,
                unidadeId
        );

        // recupera os pedidos da unidade
        List<Pedido> pedidos = canalPedido == null
                ? pedidoRepository
                .findByUnidadeIdOrderByDataCriacaoDesc(unidadeId)
                : pedidoRepository
                .findByUnidadeIdAndCanalPedidoOrderByDataCriacaoDesc(
                        unidadeId,
                        canalPedido
                );

        // converte os pedidos encontrados
        return pedidos.stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // lista os pedidos associados ao próprio cliente autenticado
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarMeusPedidos(
            CanalPedido canalPedido,
            String emailUsuario
    ) {

        // recupera o usuário autenticado
        Usuario usuario = buscarUsuarioAtivo(emailUsuario);

        // restringe o histórico pessoal aos clientes
        if (!"CLIENTE".equals(identificarPerfil(usuario))) {
            throw new AccessDeniedException(
                    "O histórico pessoal é exclusivo dos clientes."
            );
        }

        // recupera os pedidos do cliente com filtro opcional por canal
        List<Pedido> pedidos = canalPedido == null
                ? pedidoRepository
                .findByUsuarioIdOrderByDataCriacaoDesc(
                        usuario.getId()
                )
                : pedidoRepository
                .findByUsuarioIdAndCanalPedidoOrderByDataCriacaoDesc(
                        usuario.getId(),
                        canalPedido
                );

        // converte os pedidos encontrados
        return pedidos.stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // recupera um pedido específico respeitando as permissões
    @Transactional(readOnly = true)
    public PedidoResponseDTO buscarPorId(
            Long id,
            String emailUsuario
    ) {

        // recupera o usuário autenticado e identifica seu perfil
        Usuario usuario = buscarUsuarioAtivo(emailUsuario);
        String perfil = identificarPerfil(usuario);

        // recupera o pedido solicitado
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Pedido não encontrado."
                ));

        // clientes podem consultar somente seus próprios pedidos
        if ("CLIENTE".equals(perfil)) {

            if (pedido.getUsuario() == null
                    || !pedido.getUsuario().getId()
                    .equals(usuario.getId())) {

                throw new NoSuchElementException(
                        "Pedido não encontrado."
                );
            }

        } else {

            // funcionários devem possuir acesso à unidade do pedido
            permissaoUnidadeService.validarAcesso(
                    emailUsuario,
                    pedido.getUnidade().getId()
            );
        }

        // retorna os dados do pedido autorizado
        return converterParaResponseDTO(pedido);
    }

    // recupera um usuário existente e ativo
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

    // identifica e valida o perfil exclusivo do usuário
    private String identificarPerfil(Usuario usuario) {

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

        return perfis.iterator().next();
    }

    // verifica se o valor monetário pode ser armazenado no banco
    private void validarLimiteMonetario(BigDecimal valor) {

        if (valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException(
                    "O valor do pedido excede o limite permitido."
            );
        }
    }

    // converte a entidade ItemPedido para o DTO de resposta
    private ItemPedidoResponseDTO converterItemParaResponseDTO(
            ItemPedido item
    ) {

        return new ItemPedidoResponseDTO(
                item.getId(),
                item.getProdutoUnidade().getId(),
                item.getNomeProduto(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal()
        );
    }

    // converte a entidade Pedido para o DTO de resposta
    private PedidoResponseDTO converterParaResponseDTO(
            Pedido pedido
    ) {

        // converte todos os itens associados ao pedido
        List<ItemPedidoResponseDTO> itens = pedido.getItens().stream()
                .map(this::converterItemParaResponseDTO)
                .toList();

        // permite representar pedidos sem usuário associado
        Long usuarioId = pedido.getUsuario() == null
                ? null
                : pedido.getUsuario().getId();

        // monta a resposta sem expor as entidades diretamente
        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getUnidade().getId(),
                pedido.getUnidade().getNome(),
                usuarioId,
                pedido.getCanalPedido(),
                pedido.getStatus(),
                pedido.getValorTotal(),
                itens,
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao()
        );
    }
}