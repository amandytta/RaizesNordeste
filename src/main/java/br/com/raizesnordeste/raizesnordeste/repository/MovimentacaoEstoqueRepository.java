package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.MovimentacaoEstoque;
import br.com.raizesnordeste.raizesnordeste.enums.TipoMovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoEstoqueRepository
        extends JpaRepository<MovimentacaoEstoque, Long> {

    List<MovimentacaoEstoque>
    findByProdutoUnidadeIdOrderByDataMovimentacaoDesc(
            Long produtoUnidadeId
    );

    List<MovimentacaoEstoque>
    findByProdutoUnidadeUnidadeIdOrderByDataMovimentacaoDesc(
            Long unidadeId
    );

    List<MovimentacaoEstoque>
    findByTipoOrderByDataMovimentacaoDesc(
            TipoMovimentacaoEstoque tipo
    );

    List<MovimentacaoEstoque>
    findAllByOrderByDataMovimentacaoDesc();

}