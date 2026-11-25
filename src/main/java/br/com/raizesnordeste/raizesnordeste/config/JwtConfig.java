package br.com.raizesnordeste.raizesnordeste.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

// centraliza as configuracoes de assinatura e validacao dos tokens JWT
@Configuration
public class JwtConfig {

    // recupera a chave secreta e cria a chave criptografica
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

    // configura o componente responsavel pela assinatura dos tokens
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {

        return new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtSecretKey)
        );
    }

    // configura o componente responsavel pela validacao dos tokens
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${app.jwt.issuer}") String emissor) {

        // define a chave e o algoritmo aceito na validacao
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // configura a validacao do emissor e das restricoes temporais
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(emissor)
        );

        return decoder;
    }

    // configura a conversao dos perfis presentes no token JWT
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        // cria o conversor responsavel pelas autoridades do usuario
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // define a claim que armazena os perfis de acesso
        authoritiesConverter.setAuthoritiesClaimName("roles");

        // remove o prefixo padrao, pois nossos perfis ja possuem ROLE_
        authoritiesConverter.setAuthorityPrefix("");

        // cria o conversor responsavel pela autenticacao JWT
        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        // associa o conversor de autoridades a autenticacao JWT
        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        // define a claim utilizada para identificar o usuario
        authenticationConverter.setPrincipalClaimName("sub");

        return authenticationConverter;
    }
}