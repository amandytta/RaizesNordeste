package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.UsuarioUnidade;
import br.com.raizesnordeste.raizesnordeste.entity.UsuarioUnidadeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// responsável pelo acesso aos vínculos entre usuários e unidades
public interface UsuarioUnidadeRepository
        extends JpaRepository<UsuarioUnidade, UsuarioUnidadeId> {

    // lista todos os vínculos de um determinado usuário
    List<UsuarioUnidade> findByUsuarioId(Long usuarioId);

    // lista todos os vínculos associados a uma determinada unidade
    List<UsuarioUnidade> findByUnidadeId(Long unidadeId);

    // lista exclusivamente os vínculos ativos de um usuário
    List<UsuarioUnidade> findByUsuarioIdAndAtivoTrue(Long usuarioId);

    // lista exclusivamente os vínculos ativos de uma unidade
    List<UsuarioUnidade> findByUnidadeIdAndAtivoTrue(Long unidadeId);

    // verifica se existe um vínculo ativo entre o usuário e a unidade
    boolean existsByUsuarioIdAndUnidadeIdAndAtivoTrue(
            Long usuarioId,
            Long unidadeId
    );

    // conta a quantidade de vínculos ativos de um usuário
    long countByUsuarioIdAndAtivoTrue(Long usuarioId);
}