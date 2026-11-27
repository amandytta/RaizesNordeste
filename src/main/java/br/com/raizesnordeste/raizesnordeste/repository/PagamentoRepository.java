package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.Pagamento;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPagamento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// disponibiliza as operações de persistência e consulta dos pagamentos
public interface PagamentoRepository
        extends JpaRepository<Pagamento, Long> {

    // recupera o histórico de pagamentos de um pedido
    // ordena as tentativas da mais recente para a mais antiga
    List<Pagamento> findByPedidoIdOrderByDataCriacaoDescIdDesc(
            Long pedidoId
    );

    // recupera um pagamento pelo código único da transação
    Optional<Pagamento> findByCodigoTransacao(
            String codigoTransacao
    );

    // verifica se o pedido já possui um pagamento com determinado status
    // auxilia na prevenção de pagamentos aprovados em duplicidade
    boolean existsByPedidoIdAndStatus(
            Long pedidoId,
            StatusPagamento status
    );
}