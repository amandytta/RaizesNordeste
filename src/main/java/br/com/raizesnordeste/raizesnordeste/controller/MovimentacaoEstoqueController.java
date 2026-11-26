package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.MovimentacaoEstoqueRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.MovimentacaoEstoqueResponseDTO;
import br.com.raizesnordeste.raizesnordeste.enums.TipoMovimentacaoEstoque;
import br.com.raizesnordeste.raizesnordeste.service.MovimentacaoEstoqueService;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;


@RestController
@RequestMapping("/api/movimentacoes-estoque")
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService movimentacaoEstoqueService;

    // injeta o serviço responsável pelas movimentações de estoque
    public MovimentacaoEstoqueController(
            MovimentacaoEstoqueService movimentacaoEstoqueService) {

        this.movimentacaoEstoqueService = movimentacaoEstoqueService;
    }

    // lista o histórico global de movimentações
    // operação exclusiva de administradores
    @GetMapping
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarTodas(
            Authentication authentication) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(movimentacaoEstoqueService.listarTodas(
                        authentication.getName()
                ));
    }

    // busca uma movimentação pelo identificador
    // o serviço verifica o acesso à unidade correspondente
    @GetMapping("/{id}")
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> buscarPorId(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(movimentacaoEstoqueService.buscarPorId(
                        id,
                        authentication.getName()
                ));
    }

    // lista as movimentações de um produto em determinada unidade
    @GetMapping("/produto-unidade/{produtoUnidadeId}")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>>
    listarPorProdutoUnidade(
            @PathVariable Long produtoUnidadeId,
            Authentication authentication) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(movimentacaoEstoqueService.listarPorProdutoUnidade(
                        produtoUnidadeId,
                        authentication.getName()
                ));
    }

    // lista o histórico de uma unidade autorizada
    @GetMapping("/unidade/{unidadeId}")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>>
    listarPorUnidade(
            @PathVariable Long unidadeId,
            Authentication authentication) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(movimentacaoEstoqueService.listarPorUnidade(
                        unidadeId,
                        authentication.getName()
                ));
    }

    // filtra globalmente as movimentações por tipo
    // operação exclusiva de administradores
    @GetMapping("/tipo")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>>
    listarPorTipo(
            @RequestParam TipoMovimentacaoEstoque tipo,
            Authentication authentication) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(movimentacaoEstoqueService.listarPorTipo(
                        tipo,
                        authentication.getName()
                ));
    }

    // registra uma movimentação de estoque
    @PostMapping
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> registrar(
            @Valid @RequestBody MovimentacaoEstoqueRequestDTO dto,
            Authentication authentication) {

        // recupera a identidade do usuário autenticado pelo JWT
        String emailUsuario = authentication.getName();

        // executa a operação respeitando as permissões do usuário
        MovimentacaoEstoqueResponseDTO movimentacao =
                movimentacaoEstoqueService.registrar(dto, emailUsuario);

        // define a localização do recurso criado
        URI location = URI.create(
                "/api/movimentacoes-estoque/" + movimentacao.id()
        );

        // retorna HTTP 201 e impede o armazenamento da resposta em cache
        return ResponseEntity.created(location)
                .cacheControl(CacheControl.noStore())
                .body(movimentacao);
    }
}