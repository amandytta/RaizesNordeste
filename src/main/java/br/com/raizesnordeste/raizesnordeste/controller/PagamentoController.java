package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.PagamentoRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.PagamentoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.service.PagamentoService;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// disponibiliza os endpoints relacionados aos pagamentos dos pedidos
@RestController
@RequestMapping("/api/pedidos/{pedidoId}/pagamentos")
public class PagamentoController {

    // serviço responsável pelas regras de negócio dos pagamentos
    private final PagamentoService pagamentoService;

    // realiza a injeção da dependência pelo construtor
    public PagamentoController(
            PagamentoService pagamentoService
    ) {
        this.pagamentoService = pagamentoService;
    }

    // processa uma nova tentativa de pagamento simulado
    @PostMapping
    public ResponseEntity<PagamentoResponseDTO> processar(
            @PathVariable Long pedidoId,
            @Valid @RequestBody PagamentoRequestDTO dto,
            Authentication authentication
    ) {

        // recupera o e-mail do usuário autenticado
        String emailUsuario = authentication.getName();

        // solicita o processamento da tentativa de pagamento
        PagamentoResponseDTO pagamento =
                pagamentoService.processar(
                        pedidoId,
                        dto,
                        emailUsuario
                );

        // define a localização do recurso criado
        URI location = URI.create(
                "/api/pedidos/"
                        + pedidoId
                        + "/pagamentos/"
                        + pagamento.id()
        );

        // retorna HTTP 201 e impede o armazenamento em cache
        return ResponseEntity.created(location)
                .cacheControl(CacheControl.noStore())
                .body(pagamento);
    }

    // consulta o histórico de pagamentos de um pedido
    @GetMapping
    public ResponseEntity<List<PagamentoResponseDTO>> listarPorPedido(
            @PathVariable Long pedidoId,
            Authentication authentication
    ) {

        // recupera as tentativas de pagamento autorizadas
        List<PagamentoResponseDTO> pagamentos =
                pagamentoService.listarPorPedido(
                        pedidoId,
                        authentication.getName()
                );

        // retorna HTTP 200 sem permitir armazenamento em cache
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pagamentos);
    }

    // consulta uma tentativa específica de pagamento
    @GetMapping("/{pagamentoId}")
    public ResponseEntity<PagamentoResponseDTO> buscarPorId(
            @PathVariable Long pedidoId,
            @PathVariable Long pagamentoId,
            Authentication authentication
    ) {

        // recupera o pagamento após a validação das permissões
        PagamentoResponseDTO pagamento =
                pagamentoService.buscarPorId(
                        pedidoId,
                        pagamentoId,
                        authentication.getName()
                );

        // retorna os dados da transação sem armazenamento em cache
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pagamento);
    }
}