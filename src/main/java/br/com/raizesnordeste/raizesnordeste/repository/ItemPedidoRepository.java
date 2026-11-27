package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.ItemPedido;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// disponibiliza as operações de persistência e consulta dos itens de pedido
public interface ItemPedidoRepository
        extends JpaRepository<ItemPedido, Long> {

    // recupera todos os itens associados a um determinado pedido
    List<ItemPedido> findByPedidoIdOrderByIdAsc(Long pedidoId);
}