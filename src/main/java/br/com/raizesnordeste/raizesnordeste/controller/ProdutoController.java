package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.ProdutoRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.ProdutoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.service.ProdutoService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listarTodos() {

        return ResponseEntity.ok(
                produtoService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                produtoService.buscarPorId(id)
        );
    }

    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> cadastrar(
            @Valid @RequestBody ProdutoRequestDTO dto
    ) {

        ProdutoResponseDTO produto = produtoService.cadastrar(dto);

        URI localizacao = URI.create(
                "/api/produtos/" + produto.id()
        );

        return ResponseEntity.created(localizacao).body(produto);
    }
}