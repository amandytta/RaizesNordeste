package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.ProdutoUnidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// repositório responsável pela persistência e consulta das associações entre produtos e unidades
public interface ProdutoUnidadeRepository
        extends JpaRepository<ProdutoUnidade, Long> {

    // lista todos os produtos associados a uma determinada unidade
    List<ProdutoUnidade> findByUnidadeId(Long unidadeId);

    // lista todas as associações de um determinado produto
    List<ProdutoUnidade> findByProdutoId(Long produtoId);

    // lista os produtos marcados como disponíveis em uma unidade
    // utilizado na consulta pública do cardápio
    List<ProdutoUnidade> findByUnidadeIdAndDisponivelTrue(
            Long unidadeId
    );

    // busca uma associação específica entre produto e unidade
    Optional<ProdutoUnidade> findByProdutoIdAndUnidadeId(
            Long produtoId,
            Long unidadeId
    );

    // verifica se o produto já está cadastrado na unidade
    // utilizado para impedir associações duplicadas
    boolean existsByProdutoIdAndUnidadeId(
            Long produtoId,
            Long unidadeId
    );

    // busca uma associação aplicando um bloqueio pessimista de escrita
    // impede que outras transações alterem simultaneamente o mesmo registro enquanto o estoque está sendo atualizado
    // o bloqueio permanece ativo até a conclusão ou reversão da transação iniciada na camada Service
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pu FROM ProdutoUnidade pu WHERE pu.id = :id")
    Optional<ProdutoUnidade> findByIdComBloqueio(
            @Param("id") Long id
    );
}