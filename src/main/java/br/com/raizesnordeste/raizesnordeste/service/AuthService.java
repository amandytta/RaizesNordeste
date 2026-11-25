package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.LoginRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.LoginResponseDTO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

// responsavel pela autenticacao dos usuarios da aplicacao
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    // injeta as dependencias necessarias para realizar o login
    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // autentica as credenciais e gera o token JWT
    public LoginResponseDTO autenticar(LoginRequestDTO dto) {

        // garante que a senha respeite o limite de 72 bytes do BCrypt
        if (dto.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }

        // cria uma solicitacao de autenticacao com e-mail e senha
        Authentication solicitacao =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        dto.email(),
                        dto.senha()
                );

        // valida as credenciais utilizando o Spring Security
        Authentication autenticacao =
                authenticationManager.authenticate(solicitacao);

        // gera o token somente apos a autenticacao bem-sucedida
        return jwtService.gerarToken(autenticacao);
    }
}