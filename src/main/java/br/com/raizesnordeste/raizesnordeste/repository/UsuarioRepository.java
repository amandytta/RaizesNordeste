package br.com.raizesnordeste.raizesnordeste.repository;

import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import jakarta.persistence.LockModeType;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    // busca um usuário com bloqueio pessimista para operações concorrentes
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findByIdComBloqueio(@Param("id") Long id);
}