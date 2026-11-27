package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.Consentimento;
import br.com.raizesnordeste.raizesnordeste.enums.FinalidadeConsentimento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// disponibiliza as operações de persistência e consulta dos consentimentos
public interface ConsentimentoRepository
        extends JpaRepository<Consentimento, Long> {

    // recupera todo o histórico de consentimentos de um usuário
    List<Consentimento> findByUsuarioIdOrderByDataRegistroDescIdDesc(
            Long usuarioId
    );

    // recupera o histórico de uma finalidade específica
    List<Consentimento> findByUsuarioIdAndFinalidadeOrderByDataRegistroDescIdDesc(
            Long usuarioId,
            FinalidadeConsentimento finalidade
    );

    // recupera a decisão mais recente do usuário para uma finalidade
    Optional<Consentimento> findFirstByUsuarioIdAndFinalidadeOrderByDataRegistroDescIdDesc(
            Long usuarioId,
            FinalidadeConsentimento finalidade
    );
}