package br.com.raizesnordeste.raizesnordeste.config;

import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

// centraliza as configurações de assinatura e validação dos tokens JWT
@Configuration
public class JwtConfig {

    // recupera a chave secreta e cria a chave criptográfica
    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.jwt.secret}") String segredo) {

        // decodifica a chave armazenada em Base64
        byte[] chaveDecodificada = Base64.getDecoder().decode(segredo);

        // garante que a chave possua exatamente 256 bits
        if (chaveDecodificada.length != 32) {
            throw new IllegalStateException(
                    "A chave JWT deve possuir exatamente 256 bits."
            );
        }

        // cria a chave utilizada pelo algoritmo HS256
        return new SecretKeySpec(chaveDecodificada, "HmacSHA256");
    }

    // configura o componente responsável pela assinatura dos tokens
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {

        return new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtSecretKey)
        );
    }

    // configura o componente responsável pela validação dos tokens
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${app.jwt.issuer}") String emissor) {

        // define a chave e o algoritmo aceito na validação
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // configura a validação do emissor e das restrições temporais
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(emissor)
        );

        return decoder;
    }

    // configura a conversão e a revalidação das autoridades do JWT
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            UsuarioRepository usuarioRepository) {

        // cria o conversor responsável pelas autoridades do token
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // define a claim que armazena os perfis de acesso
        authoritiesConverter.setAuthoritiesClaimName("roles");

        // remove o prefixo padrão, pois nossos perfis já possuem ROLE_
        authoritiesConverter.setAuthorityPrefix("");

        // cria o conversor responsável pela autenticação JWT
        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        // revalida as permissões do usuário a cada autenticação por JWT
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {

            // recupera o e-mail armazenado no identificador do token
            String email = jwt.getSubject();

            // rejeita tokens sem identificação válida do usuário
            if (email == null || email.isBlank()) {
                throw criarExcecaoTokenInvalido();
            }

            // consulta os dados atuais do usuário no banco
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(this::criarExcecaoTokenInvalido);

            // impede a utilização de tokens pertencentes a usuários inativos
            if (!Boolean.TRUE.equals(usuario.getAtivo())) {
                throw criarExcecaoTokenInvalido();
            }

            // recupera as autoridades presentes no token recebido
            Collection<GrantedAuthority> autoridadesToken =
                    authoritiesConverter.convert(jwt);

            // converte as autoridades do token em um conjunto de nomes
            Set<String> perfisToken = autoridadesToken.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // recupera os perfis atualmente associados ao usuário no banco
            Set<String> perfisAtuais = usuario.getPerfis().stream()
                    .map(perfil -> "ROLE_" + perfil.getNome())
                    .collect(Collectors.toSet());

            // garante que o usuário possua exatamente um perfil válido
            if (perfisAtuais.size() != 1) {
                throw criarExcecaoTokenInvalido();
            }

            // define os perfis reconhecidos pela aplicação
            Set<String> perfisPermitidos = Set.of(
                    "ROLE_ADMIN",
                    "ROLE_GERENTE",
                    "ROLE_ATENDENTE",
                    "ROLE_CLIENTE"
            );

            // impede a autenticação com perfis desconhecidos
            if (!perfisPermitidos.containsAll(perfisAtuais)) {
                throw criarExcecaoTokenInvalido();
            }

            // rejeita tokens cujas permissões não correspondem mais ao banco
            // exige uma nova autenticação após alterações de perfil
            if (!perfisToken.equals(perfisAtuais)) {
                throw criarExcecaoTokenInvalido();
            }

            // retorna as autoridades somente após todas as validações
            return autoridadesToken;
        });

        // define a claim utilizada para identificar o usuário
        authenticationConverter.setPrincipalClaimName("sub");

        return authenticationConverter;
    }

    // cria uma exceção padronizada para tokens que perderam sua validade
    private OAuth2AuthenticationException criarExcecaoTokenInvalido() {

        // utiliza o código de erro reconhecido pelo padrão OAuth2
        OAuth2Error erro = new OAuth2Error(
                "invalid_token",
                "Token inválido ou permissões desatualizadas.",
                null
        );

        // encaminha a falha para o tratamento de autenticação do Spring Security
        return new OAuth2AuthenticationException(erro);
    }
}