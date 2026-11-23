package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.CategoriaRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.CategoriaResponseDTO;
import br.com.raizesnordeste.raizesnordeste.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(categoriaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> cadastrar(
            @Valid @RequestBody CategoriaRequestDTO dto
    ) {
        CategoriaResponseDTO categoria = categoriaService.cadastrar(dto);

        URI localizacao = URI.create("/api/categorias/" + categoria.id());

        return ResponseEntity.created(localizacao).body(categoria);
    }
}