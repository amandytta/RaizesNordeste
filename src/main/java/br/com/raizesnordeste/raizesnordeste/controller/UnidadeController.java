package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.UnidadeRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UnidadeResponseDTO;
import br.com.raizesnordeste.raizesnordeste.service.UnidadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @GetMapping
    public ResponseEntity<List<UnidadeResponseDTO>> listarTodas() {
        return ResponseEntity.ok(unidadeService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadeResponseDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(unidadeService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UnidadeResponseDTO> cadastrar(
            @Valid @RequestBody UnidadeRequestDTO dto) {

        UnidadeResponseDTO unidade = unidadeService.cadastrar(dto);

        URI localizacao = URI.create("/api/unidades/" + unidade.id());

        return ResponseEntity
                .created(localizacao)
                .body(unidade);
    }
}