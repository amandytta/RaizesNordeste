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

                                // restringe as operações administrativas de usuários ao perfil ADMIN
                                .requestMatchers(
                                        "/api/usuarios",
                                        "/api/usuarios/**"
                                ).hasRole("ADMIN")

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

                                // somente administradores podem cadastrar unidades, categorias, produtos e associações
                                .requestMatchers(HttpMethod.POST,
                                        "/api/unidades",
                                        "/api/categorias",
                                        "/api/produtos",
                                        "/api/produtos-unidades"
                                ).hasRole("ADMIN")

                                // restringe o histórico global aos administradores
                                .requestMatchers(HttpMethod.GET,
                                        "/api/movimentacoes-estoque",
                                        "/api/movimentacoes-estoque/tipo"
                                ).hasRole("ADMIN")

                                // permite consultas específicas aos funcionários autorizados
                                // o serviço verifica o vínculo com a unidade solicitada
                                .requestMatchers(HttpMethod.GET,
                                        "/api/movimentacoes-estoque/*",
                                        "/api/movimentacoes-estoque/produto-unidade/*",
                                        "/api/movimentacoes-estoque/unidade/*"
                                ).hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")

                                // permite entradas e saídas aos administradores e gerentes
                                // o serviço verifica a autorização para a unidade correspondente
                                .requestMatchers(HttpMethod.POST,
                                        "/api/movimentacoes-estoque"
                                ).hasAnyRole("ADMIN", "GERENTE")

                                // bloqueia outras operações não previstas no módulo de estoque
                                .requestMatchers(
                                        "/api/movimentacoes-estoque",
                                        "/api/movimentacoes-estoque/**"
                                ).denyAll()

                                // permite a criação de pedidos pelos perfis reconhecidos
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/pedidos"
                                ).hasAnyRole("ADMIN", "GERENTE", "ATENDENTE", "CLIENTE")

                                // restringe o histórico global aos administradores
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/pedidos"
                                ).hasRole("ADMIN")

                                // restringe o histórico pessoal aos clientes
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/pedidos/meus"
                                ).hasRole("CLIENTE")

                                // permite consultas por unidade aos funcionários autorizados
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/pedidos/unidade/*"
                                ).hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")

                                // permite consultar pedidos específicos
                                // a autorização individual será validada pelo service
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/pedidos/*"
                                ).hasAnyRole("ADMIN", "GERENTE", "ATENDENTE", "CLIENTE")

                                // permite o processamento de pagamentos pelos perfis autorizados
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/pedidos/*/pagamentos"
                                ).hasAnyRole(
                                        "ADMIN",
                                        "GERENTE",
                                        "ATENDENTE",
                                        "CLIENTE"
                                )

                                // permite a consulta do histórico de pagamentos de um pedido
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/pedidos/*/pagamentos"
                                ).hasAnyRole(
                                        "ADMIN",
                                        "GERENTE",
                                        "ATENDENTE",
                                        "CLIENTE"
                                )

                                // permite a consulta de uma tentativa específica de pagamento
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/pedidos/*/pagamentos/*"
                                ).hasAnyRole(
                                        "ADMIN",
                                        "GERENTE",
                                        "ATENDENTE",
                                        "CLIENTE"
                                )

                                // permite a atualização operacional pelos funcionários autorizados
                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/pedidos/*/status"
                                ).hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")

                                // permite o cancelamento de pedidos pelos perfis autorizados
                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/pedidos/*/cancelamento"
                                ).hasAnyRole("ADMIN", "GERENTE", "ATENDENTE", "CLIENTE")

                                // bloqueia operações de pedidos que ainda não foram previstas
                                .requestMatchers(
                                        "/api/pedidos",
                                        "/api/pedidos/**"
                                ).denyAll()

                                // permite que usuários autenticados registrem seus consentimentos
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/consentimentos"
                                ).hasAnyRole("ADMIN", "GERENTE", "ATENDENTE", "CLIENTE")

                                // permite a consulta do próprio histórico de consentimentos
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/consentimentos/meus"
                                ).hasAnyRole("ADMIN", "GERENTE", "ATENDENTE", "CLIENTE")

                                // permite a consulta da situação atual dos próprios consentimentos
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/consentimentos/meus/atuais"
                                ).hasAnyRole("ADMIN", "GERENTE", "ATENDENTE", "CLIENTE")

                                // bloqueia operações de consentimento não autorizadas
                                .requestMatchers(
                                        "/api/consentimentos",
                                        "/api/consentimentos/**"
                                ).denyAll()

                                // restringe a consulta de auditoria aos administradores
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/auditorias",
                                        "/api/auditorias/*"
                                ).hasRole("ADMIN")

                                // bloqueia operações de auditoria não autorizadas
                                .requestMatchers(
                                        "/api/auditorias",
                                        "/api/auditorias/**"
                                ).denyAll()

                                // exige autenticação para as demais requisicoes
                                .anyRequest().authenticated()
                )

                // desabilita explicitamente a autenticação HTTP Basic
                .httpBasic(basic -> basic.disable())

                // desabilita a autenticação por formulario
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