package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.LoginResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// responsavel pela geracao dos tokens JWT da aplicacao
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String emissor;
    private final long expiracao;

    // injeta o componente de assinatura e as propriedades JWT
    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.issuer}") String emissor,
            @Value("${app.jwt.expiration}") long expiracao) {

        this.jwtEncoder = jwtEncoder;
        this.emissor = emissor;
        this.expiracao = expiracao;
    }

    // gera um token para um usuario previamente autenticado
    public LoginResponseDTO gerarToken(Authentication authentication) {

        // registra o instante atual da emissao do token
        Instant agora = Instant.now();

        // recupera exclusivamente os perfis de acesso do usuario
        List<String> perfis = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)

                // mantém apenas as autoridades que representam perfis
                // exclui fatores de autenticacao, como FACTOR_PASSWORD
                .filter(autoridade -> autoridade.startsWith("ROLE_"))

                // converte os perfis filtrados em uma lista
                .toList();

        // define as informacoes que serao armazenadas no token
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emissor)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(expiracao))
                .subject(authentication.getName())
                .claim("roles", perfis)
                .build();

        // define explicitamente o algoritmo de assinatura HS256
        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        // gera e assina o token utilizando a chave configurada
        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        // retorna o token, o tipo de autenticacao e sua validade
        return new LoginResponseDTO(
                token,
                "Bearer",
                expiracao
        );
    }
}