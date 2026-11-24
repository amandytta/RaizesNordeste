package br.com.raizesnordeste.raizesnordeste;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// carrega o contexto completo da aplicação Spring Boot
@SpringBootTest

// ativa o perfil exclusivo do ambiente de testes
@ActiveProfiles("test")
class RaizesNordesteApplicationTests {

    // verifica se o contexto da aplicação é carregado corretamente
    @Test
    void contextLoads() {
    }
}