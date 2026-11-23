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
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/unidades",
                                "/api/unidades/*",
                                "/api/categorias",
                                "/api/categorias/*",
                                "/api/produtos",
                                "/api/produtos/*"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/unidades",
                                "/api/categorias",
                                "/api/produtos"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .httpBasic(basic -> {});

        return http.build();
    }
}