package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.ConsentimentoAtualResponseDTO;
import br.com.raizesnordeste.raizesnordeste.dto.ConsentimentoRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.ConsentimentoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.enums.FinalidadeConsentimento;
import br.com.raizesnordeste.raizesnordeste.service.ConsentimentoService;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// disponibiliza os endpoints relacionados aos consentimentos LGPD
@RestController
@RequestMapping("/api/consentimentos")
public class ConsentimentoController {

    // serviço responsável pelas regras de negócio dos consentimentos
    private final ConsentimentoService consentimentoService;

    // realiza a injeção da dependência pelo construtor
    public ConsentimentoController(
            ConsentimentoService consentimentoService
    ) {
        this.consentimentoService = consentimentoService;
    }

    // registra uma concessão, recusa ou revogação de consentimento
    @PostMapping
    public ResponseEntity<ConsentimentoResponseDTO> registrar(
            @Valid @RequestBody ConsentimentoRequestDTO dto,
            Authentication authentication
    ) {

        // recupera o e-mail do usuário autenticado
        String emailUsuario = authentication.getName();

        // registra a decisão no histórico de consentimentos
        ConsentimentoResponseDTO consentimento =
                consentimentoService.registrar(
                        dto,
                        emailUsuario
                );

        // retorna HTTP 201 sem permitir armazenamento em cache
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(consentimento);
    }

    // consulta o histórico de consentimentos do próprio usuário
    @GetMapping("/meus")
    public ResponseEntity<List<ConsentimentoResponseDTO>> listarMeus(
            @RequestParam(required = false)
            FinalidadeConsentimento finalidade,
            Authentication authentication
    ) {

        // recupera o histórico com filtro opcional por finalidade
        List<ConsentimentoResponseDTO> consentimentos =
                consentimentoService.listarMeusConsentimentos(
                        finalidade,
                        authentication.getName()
                );

        // retorna os registros sem permitir armazenamento em cache
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(consentimentos);
    }

    // consulta a situação atual de todas as finalidades de consentimento
    @GetMapping("/meus/atuais")
    public ResponseEntity<List<ConsentimentoAtualResponseDTO>> listarAtuais(
            Authentication authentication
    ) {

        // recupera as decisões atuais do usuário autenticado
        List<ConsentimentoAtualResponseDTO> consentimentos =
                consentimentoService.listarMeusConsentimentosAtuais(
                        authentication.getName()
                );

        // retorna a situação atual dos consentimentos
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(consentimentos);
    }
}