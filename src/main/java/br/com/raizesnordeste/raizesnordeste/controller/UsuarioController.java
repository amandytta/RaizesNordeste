package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.UsuarioCadastroRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioResponseDTO;
import br.com.raizesnordeste.raizesnordeste.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// disponibiliza os endpoints relacionados aos usuarios
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    // responsável pelas regras de negócio dos usuarios
    private final UsuarioService usuarioService;

    // injeta a dependencia por meio do construtor
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // disponibiliza o endpoint de cadastro publico de clientes
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponseDTO> cadastrarCliente(
            @Valid @RequestBody UsuarioCadastroRequestDTO dto
    ) {

        // encaminha os dados validados para a camada de serviço
        UsuarioResponseDTO resposta =
                usuarioService.cadastrarCliente(dto);

        // retorna HTTP 201 para indicar a criação do usuário
        // impede o armazenamento da resposta em cache, pois ela contem dados pessoais
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(resposta);
    }
}