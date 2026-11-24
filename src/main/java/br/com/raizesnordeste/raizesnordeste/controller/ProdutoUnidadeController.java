package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.ProdutoUnidadeRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.ProdutoUnidadeResponseDTO;
import br.com.raizesnordeste.raizesnordeste.service.ProdutoUnidadeService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/produtos-unidades")
public class ProdutoUnidadeController {

    private final ProdutoUnidadeService produtoUnidadeService;

    public ProdutoUnidadeController(
            ProdutoUnidadeService produtoUnidadeService
    ) {
        this.produtoUnidadeService = produtoUnidadeService;
    }

    @GetMapping
    public ResponseEntity<List<ProdutoUnidadeResponseDTO>> listarTodos() {

        return ResponseEntity.ok(
                produtoUnidadeService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoUnidadeResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                produtoUnidadeService.buscarPorId(id)
        );
    }

    @GetMapping("/cardapio/{unidadeId}")
    public ResponseEntity<List<ProdutoUnidadeResponseDTO>> listarCardapio(
            @PathVariable Long unidadeId
    ) {

        return ResponseEntity.ok(
                produtoUnidadeService.listarCardapioPorUnidade(unidadeId)
        );
    }

    @PostMapping
    public ResponseEntity<ProdutoUnidadeResponseDTO> cadastrar(
            @Valid @RequestBody ProdutoUnidadeRequestDTO dto
    ) {

        ProdutoUnidadeResponseDTO produtoUnidade =
                produtoUnidadeService.cadastrar(dto);

        URI localizacao = URI.create(
                "/api/produtos-unidades/" + produtoUnidade.id()
        );

        return ResponseEntity.created(localizacao)
                .body(produtoUnidade);
    }
}