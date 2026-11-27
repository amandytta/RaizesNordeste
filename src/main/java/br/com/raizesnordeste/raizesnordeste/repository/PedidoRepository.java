package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.Pedido;
import br.com.raizesnordeste.raizesnordeste.enums.CanalPedido;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPedido;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// disponibiliza as operações de persistência e consulta dos pedidos
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // lista todos os pedidos, do mais recente ao mais antigo
    List<Pedido> findAllByOrderByDataCriacaoDesc();

    // lista os pedidos realizados em uma determinada unidade
    List<Pedido> findByUnidadeIdOrderByDataCriacaoDesc(Long unidadeId);

    // lista os pedidos associados a um determinado usuário
    List<Pedido> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

    // lista os pedidos realizados por um determinado canal
    List<Pedido> findByCanalPedidoOrderByDataCriacaoDesc(
            CanalPedido canalPedido
    );

    // lista os pedidos de uma unidade filtrados pelo canal
    List<Pedido> findByUnidadeIdAndCanalPedidoOrderByDataCriacaoDesc(
            Long unidadeId,
            CanalPedido canalPedido
    );

    // lista os pedidos de um usuário filtrados pelo canal
    List<Pedido> findByUsuarioIdAndCanalPedidoOrderByDataCriacaoDesc(
            Long usuarioId,
            CanalPedido canalPedido
    );

    // lista os pedidos de uma unidade que possuem determinado status
    List<Pedido> findByUnidadeIdAndStatusOrderByDataCriacaoDesc(
            Long unidadeId,
            StatusPedido status
    );

    // recupera um pedido com bloqueio exclusivo para atualização
    // evita alterações concorrentes durante pagamentos e mudanças de status
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> findByIdComBloqueio(@Param("id") Long id);
}