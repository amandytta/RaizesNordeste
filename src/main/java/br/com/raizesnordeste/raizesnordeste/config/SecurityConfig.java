package br.com.raizesnordeste.raizesnordeste.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // permite consultas públicas de unidades, categorias, produtos e cardápios
                        .requestMatchers(HttpMethod.GET,
                                "/api/unidades",
                                "/api/unidades/*",
                                "/api/categorias",
                                "/api/categorias/*",
                                "/api/produtos",
                                "/api/produtos/*",
                                "/api/produtos-unidades/cardapio/*"
                        ).permitAll()

                        // restringe as consultas administrativas de produtos por unidade ao perfil ADMIN
                        .requestMatchers(HttpMethod.GET,
                                "/api/produtos-unidades",
                                "/api/produtos-unidades/*"
                        ).hasRole("ADMIN")

                        // somente administradores podem cadastrar unidades, categorias, produtos e associações
                        .requestMatchers(HttpMethod.POST,
                                "/api/unidades",
                                "/api/categorias",
                                "/api/produtos",
                                "/api/produtos-unidades"
                        ).hasRole("ADMIN")

                        // protege todos os endpoints de movimentação de estoque
                        // somente usuários com o perfil ADMIN podem consultar o histórico ou registrar movimentações
                        .requestMatchers(
                                "/api/movimentacoes-estoque",
                                "/api/movimentacoes-estoque/**"
                        ).hasRole("ADMIN")

                        // exige autenticação para as demais requisições
                        .anyRequest().authenticated()
                )

                .httpBasic(basic -> {});

        return http.build();
    }
}