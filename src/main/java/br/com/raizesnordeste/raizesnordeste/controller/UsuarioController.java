package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.FuncionarioCadastroRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioCadastroRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioResponseDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioUnidadeRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioUnidadeResponseDTO;
import br.com.raizesnordeste.raizesnordeste.service.UsuarioService;
import br.com.raizesnordeste.raizesnordeste.service.UsuarioUnidadeService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// responsável pelos endpoints de cadastro e gerenciamento de usuários
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    // serviço responsável pelo gerenciamento de usuários
    private final UsuarioService usuarioService;

    // serviço responsável pelo gerenciamento dos vínculos com unidades
    private final UsuarioUnidadeService usuarioUnidadeService;

    // realiza a injeção das dependências pelo construtor
    public UsuarioController(
            UsuarioService usuarioService,
            UsuarioUnidadeService usuarioUnidadeService) {

        this.usuarioService = usuarioService;
        this.usuarioUnidadeService = usuarioUnidadeService;
    }

    // disponibiliza o cadastro público de clientes
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponseDTO> cadastrarCliente(
            @Valid @RequestBody UsuarioCadastroRequestDTO dto) {

        // realiza o cadastro do cliente
        UsuarioResponseDTO resposta = usuarioService.cadastrarCliente(dto);

        // retorna HTTP 201 e impede o armazenamento da resposta em cache
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(resposta);
    }

    // lista os usuários cadastrados no sistema
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {

        // retorna a lista de usuários sem expor suas senhas
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(usuarioService.listarTodos());
    }

    // consulta um usuário pelo seu identificador
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(
            @PathVariable Long id) {

        // retorna os dados do usuário encontrado
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(usuarioService.buscarPorId(id));
    }

    // disponibiliza o cadastro administrativo de funcionários
    @PostMapping("/funcionarios")
    public ResponseEntity<UsuarioResponseDTO> cadastrarFuncionario(
            @Valid @RequestBody FuncionarioCadastroRequestDTO dto) {

        // realiza o cadastro do funcionário
        UsuarioResponseDTO resposta =
                usuarioService.cadastrarFuncionario(dto);

        // define o endereço do recurso criado
        URI localizacao = URI.create(
                "/api/usuarios/" + resposta.id()
        );

        // retorna HTTP 201 com a localização do novo usuário
        return ResponseEntity.created(localizacao)
                .cacheControl(CacheControl.noStore())
                .body(resposta);
    }

    // lista os vínculos ativos e inativos de um usuário
    @GetMapping("/{id}/unidades")
    public ResponseEntity<List<UsuarioUnidadeResponseDTO>> listarVinculos(
            @PathVariable Long id) {

        // retorna os vínculos associados ao usuário informado
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(usuarioUnidadeService.listarPorUsuario(id));
    }

    // estabelece ou reativa o vínculo de um funcionário com uma unidade
    @PostMapping("/{id}/unidades")
    public ResponseEntity<UsuarioUnidadeResponseDTO> vincular(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUnidadeRequestDTO dto) {

        // realiza a associação do funcionário com a unidade
        UsuarioUnidadeResponseDTO resposta =
                usuarioUnidadeService.vincular(id, dto);

        // retorna os dados do vínculo estabelecido ou reativado
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(resposta);
    }

    // desativa o vínculo entre um funcionário e uma unidade
    @DeleteMapping("/{id}/unidades/{unidadeId}")
    public ResponseEntity<UsuarioUnidadeResponseDTO> desvincular(
            @PathVariable Long id,
            @PathVariable Long unidadeId) {

        // desativa o vínculo preservando seu registro no banco
        UsuarioUnidadeResponseDTO resposta =
                usuarioUnidadeService.desvincular(id, unidadeId);

        // retorna os dados atualizados do vínculo
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(resposta);
    }
}