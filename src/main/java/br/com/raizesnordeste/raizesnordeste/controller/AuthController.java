package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.LoginRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.LoginResponseDTO;
import br.com.raizesnordeste.raizesnordeste.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// disponibiliza os endpoints de autenticacao da aplicacao
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // injeta o servico responsavel pela autenticacao
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // recebe as credenciais e retorna o token JWT
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto) {

        // autentica o usuario e gera o token de acesso
        LoginResponseDTO resposta = authService.autenticar(dto);

        // retorna HTTP 200 e impede o armazenamento da resposta em cache
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(resposta);
    }
}