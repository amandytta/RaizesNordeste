package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.Auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// disponibiliza operações de persistência e consulta de auditoria
public interface AuditoriaRepository
        extends JpaRepository<Auditoria, Long> {

    // consulta os eventos de auditoria de forma paginada
    Page<Auditoria> findAllByOrderByDataEventoDescIdDesc(
            Pageable pageable
    );

    // consulta os eventos relacionados a uma entidade
    Page<Auditoria> findByEntidadeOrderByDataEventoDescIdDesc(
            String entidade,
            Pageable pageable
    );

    // consulta os eventos relacionados a uma determinada ação
    Page<Auditoria> findByAcaoOrderByDataEventoDescIdDesc(
            String acao,
            Pageable pageable
    );

    // consulta os eventos realizados por determinado usuário
    Page<Auditoria> findByUsuarioIdOrderByDataEventoDescIdDesc(
            Long usuarioId,
            Pageable pageable
    );

    // consulta o histórico de um registro específico
    Page<Auditoria> findByEntidadeAndRegistroIdOrderByDataEventoDescIdDesc(
            String entidade,
            Long registroId,
            Pageable pageable
    );
}