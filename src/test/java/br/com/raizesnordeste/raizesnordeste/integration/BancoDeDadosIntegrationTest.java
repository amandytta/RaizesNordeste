package br.com.raizesnordeste.raizesnordeste.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

// carrega o contexto completo da aplicação Spring Boot
@SpringBootTest

// ativa exclusivamente o perfil de configuração de testes
@ActiveProfiles("test")
class BancoDeDadosIntegrationTest {

    // injeta o componente responsável pela execução de consultas SQL
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveUtilizarBancoDeDadosExclusivoDeTestes() {

        // consulta o nome do banco de dados conectado
        String bancoAtual = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        // confirma que a conexão utiliza o banco exclusivo de testes
        assertEquals("raizes_nordeste_test", bancoAtual);

        // consulta o usuário autenticado pelo servidor MySQL
        String usuarioAtual = jdbcTemplate.queryForObject(
                "SELECT CURRENT_USER()",
                String.class
        );

        // confirma que a conexão utiliza o usuário exclusivo de testes
        assertEquals("raizes_test@localhost", usuarioAtual);
    }
}