package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    Optional<Unidade> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}