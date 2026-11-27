package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.AuditoriaPaginaResponseDTO;
import br.com.raizesnordeste.raizesnordeste.dto.AuditoriaResponseDTO;
import br.com.raizesnordeste.raizesnordeste.service.AuditoriaService;

import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// disponibiliza os endpoints administrativos de auditoria
@RestController
@RequestMapping("/api/auditorias")
public class AuditoriaController {

    // serviço responsável pelas operações de auditoria
    private final AuditoriaService auditoriaService;

    // realiza a injeção da dependência pelo construtor
    public AuditoriaController(
            AuditoriaService auditoriaService
    ) {
        this.auditoriaService = auditoriaService;
    }

    // consulta os eventos com paginação e filtros opcionais
    @GetMapping
    public ResponseEntity<AuditoriaPaginaResponseDTO> listar(
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long registroId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            Authentication authentication
    ) {

        // consulta os registros de auditoria
        Page<AuditoriaResponseDTO> resultado =
                auditoriaService.listar(
                        entidade,
                        acao,
                        usuarioId,
                        registroId,
                        pagina,
                        tamanho,
                        authentication.getName()
                );

        // converte a página para o contrato de resposta da API
        AuditoriaPaginaResponseDTO resposta =
                new AuditoriaPaginaResponseDTO(
                        resultado.getContent(),
                        resultado.getNumber(),
                        resultado.getSize(),
                        resultado.getTotalElements(),
                        resultado.getTotalPages(),
                        resultado.isFirst(),
                        resultado.isLast()
                );

        // retorna os dados sem permitir armazenamento em cache
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(resposta);
    }

    // consulta um registro específico de auditoria
    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaResponseDTO> buscarPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {

        // consulta o registro solicitado
        AuditoriaResponseDTO auditoria =
                auditoriaService.buscarPorId(
                        id,
                        authentication.getName()
                );

        // protege os dados contra armazenamento em cache
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(auditoria);
    }
}