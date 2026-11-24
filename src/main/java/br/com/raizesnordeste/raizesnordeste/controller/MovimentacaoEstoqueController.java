package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.MovimentacaoEstoqueRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.MovimentacaoEstoqueResponseDTO;
import br.com.raizesnordeste.raizesnordeste.enums.TipoMovimentacaoEstoque;
import br.com.raizesnordeste.raizesnordeste.service.MovimentacaoEstoqueService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// define a classe como um controlador REST, responsável por receber requisições HTTP e retornar respostas JSON
@RestController
@RequestMapping("/api/movimentacoes-estoque")
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService movimentacaoEstoqueService;

    // injeção de dependência através do construtor
    public MovimentacaoEstoqueController(
            MovimentacaoEstoqueService movimentacaoEstoqueService
    ) {
        this.movimentacaoEstoqueService = movimentacaoEstoqueService;
    }

    // GET /api/movimentacoes-estoque
    // retorna o histórico geral de movimentações de estoque
    @GetMapping
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarTodas() {

        return ResponseEntity.ok(
                movimentacaoEstoqueService.listarTodas()
        );
    }

    // GET /api/movimentacoes-estoque/{id}
    // busca uma movimentação específica pelo seu identificador
    @GetMapping("/{id}")
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                movimentacaoEstoqueService.buscarPorId(id)
        );
    }

    // GET /api/movimentacoes-estoque/produto-unidade/{produtoUnidadeId}
    // consulta o histórico de um produto em uma unidade específica
    @GetMapping("/produto-unidade/{produtoUnidadeId}")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>>
    listarPorProdutoUnidade(
            @PathVariable Long produtoUnidadeId
    ) {

        return ResponseEntity.ok(
                movimentacaoEstoqueService.listarPorProdutoUnidade(
                        produtoUnidadeId
                )
        );
    }

    // GET /api/movimentacoes-estoque/unidade/{unidadeId}
    // consulta todas as movimentações de uma determinada unidade
    @GetMapping("/unidade/{unidadeId}")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>>
    listarPorUnidade(
            @PathVariable Long unidadeId
    ) {

        return ResponseEntity.ok(
                movimentacaoEstoqueService.listarPorUnidade(unidadeId)
        );
    }

    // GET /api/movimentacoes-estoque/tipo?tipo=ENTRADA
    // filtra o histórico pelo tipo de movimentação informado
    @GetMapping("/tipo")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>>
    listarPorTipo(
            @RequestParam TipoMovimentacaoEstoque tipo
    ) {

        return ResponseEntity.ok(
                movimentacaoEstoqueService.listarPorTipo(tipo)
        );
    }

    // POST /api/movimentacoes-estoque
    // registra uma movimentação e atualiza o saldo do estoque
    @PostMapping
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> registrar(
            @Valid @RequestBody MovimentacaoEstoqueRequestDTO dto,
            Authentication authentication
    ) {

        // obtém o e-mail do usuário autenticado pelo Spring Security
        // dessa forma, o cliente não pode escolher outro usuário como responsável pela movimentação
        String emailUsuario = authentication.getName();

        // delega as regras de negócio à camada Service
        MovimentacaoEstoqueResponseDTO movimentacao =
                movimentacaoEstoqueService.registrar(
                        dto,
                        emailUsuario
                );

        // define o endereço do novo recurso criado
        URI location = URI.create(
                "/api/movimentacoes-estoque/" + movimentacao.id()
        );

        // retorna HTTP 201 Created, o cabeçalho Location e os dados da movimentação registrada
        return ResponseEntity
                .created(location)
                .body(movimentacao);
    }
}