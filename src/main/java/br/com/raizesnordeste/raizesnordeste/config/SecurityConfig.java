package br.com.raizesnordeste.raizesnordeste.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

// centraliza as configuracoes de seguranca da aplicacao
@Configuration
public class SecurityConfig {

    // disponibiliza o BCrypt para criptografia unidirecional das senhas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // configura as regras de autenticacao e autorizacao da API
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler
    ) throws Exception {

        http
                // desabilita o CSRF, pois a API utiliza tokens enviados pelo cabecalho Authorization
                .csrf(csrf -> csrf.disable())

                // impede a criacao de sessoes de autenticacao no servidor
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // define as regras de acesso aos endpoints
                .authorizeHttpRequests(auth -> auth

                        // permite que usuarios não autenticados realizem login
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // permite o cadastro publico de novos clientes
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/cadastro").permitAll()

                        // permite consultas publicas de unidades, categorias, produtos e cardapios
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

                        // somente administradores podem cadastrar unidades, categorias, produtos e associacoes
                        .requestMatchers(HttpMethod.POST,
                                "/api/unidades",
                                "/api/categorias",
                                "/api/produtos",
                                "/api/produtos-unidades"
                        ).hasRole("ADMIN")

                        // protege todos os endpoints de movimentacao de estoque, restringindo o acesso ao perfil ADMIN
                        .requestMatchers(
                                "/api/movimentacoes-estoque",
                                "/api/movimentacoes-estoque/**"
                        ).hasRole("ADMIN")

                        // exige autenticacao para as demais requisicoes
                        .anyRequest().authenticated()
                )

                // desabilita explicitamente a autenticacao HTTP Basic
                .httpBasic(basic -> basic.disable())

                // desabilita a autenticacao por formulario
                .formLogin(form -> form.disable())


                // configura o tratamento padronizado das excecoes de seguranca
                .exceptionHandling(exception -> exception

                        // trata requisicoes sem autenticacao valida
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)

                        // trata requisicoes sem permissao suficiente
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                // habilita a autenticacao por tokens JWT
                .oauth2ResourceServer(oauth2 -> oauth2

                        // trata falhas na autenticacao dos tokens JWT
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)

                        // trata acessos negados aos recursos protegidos
                        .accessDeniedHandler(jwtAccessDeniedHandler)

                        // configura a autenticacao e a conversao dos perfis
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                );


        // constroi e retorna a cadeia de filtros de seguranca
        return http.build();
    }

    // disponibiliza o gerenciador de autenticacao do Spring Security
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        // recupera o gerenciador de autenticacao configurado pelo Spring
        return configuration.getAuthenticationManager();
    }
}