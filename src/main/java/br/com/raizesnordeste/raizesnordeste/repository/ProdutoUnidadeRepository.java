package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.ProdutoUnidade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoUnidadeRepository
        extends JpaRepository<ProdutoUnidade, Long> {

    List<ProdutoUnidade> findByUnidadeId(Long unidadeId);

    List<ProdutoUnidade> findByProdutoId(Long produtoId);

    List<ProdutoUnidade> findByUnidadeIdAndDisponivelTrue(
            Long unidadeId
    );

    Optional<ProdutoUnidade> findByProdutoIdAndUnidadeId(
            Long produtoId, Long unidadeId
    );

    boolean existsByProdutoIdAndUnidadeId(
            Long produtoId, Long unidadeId
    );
}