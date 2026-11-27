package br.com.raizesnordeste.raizesnordeste.controller;

import br.com.raizesnordeste.raizesnordeste.dto.PedidoRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.PedidoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.dto.PedidoStatusRequestDTO;
import br.com.raizesnordeste.raizesnordeste.enums.CanalPedido;
import br.com.raizesnordeste.raizesnordeste.service.PedidoService;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// disponibiliza os endpoints REST relacionados aos pedidos
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    // serviço responsável pelas regras de negócio dos pedidos
    private final PedidoService pedidoService;

    // realiza a injeção da dependência pelo construtor
    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // cadastra um novo pedido utilizando o usuário autenticado
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> cadastrar(
            @Valid @RequestBody PedidoRequestDTO dto,
            Authentication authentication
    ) {

        // recupera o e-mail do usuário a partir da autenticação
        String emailUsuario = authentication.getName();

        // cadastra o pedido e seus respectivos itens
        PedidoResponseDTO pedido = pedidoService.cadastrar(
                dto,
                emailUsuario
        );

        // define a localização do recurso criado
        URI location = URI.create(
                "/api/pedidos/" + pedido.id()
        );

        // retorna HTTP 201 e impede o armazenamento da resposta em cache
        return ResponseEntity.created(location)
                .cacheControl(CacheControl.noStore())
                .body(pedido);
    }

    // lista todos os pedidos da rede
    // permite filtrar os pedidos pelo canal de atendimento
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos(
            @RequestParam(required = false) CanalPedido canalPedido,
            Authentication authentication
    ) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pedidoService.listarTodos(
                        canalPedido,
                        authentication.getName()
                ));
    }

    // recupera um pedido específico respeitando as permissões
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pedidoService.buscarPorId(
                        id,
                        authentication.getName()
                ));
    }

    // lista os pedidos de uma unidade autorizada
    // permite utilizar o canal como filtro opcional
    @GetMapping("/unidade/{unidadeId}")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorUnidade(
            @PathVariable Long unidadeId,
            @RequestParam(required = false) CanalPedido canalPedido,
            Authentication authentication
    ) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pedidoService.listarPorUnidade(
                        unidadeId,
                        canalPedido,
                        authentication.getName()
                ));
    }

    // lista os pedidos do próprio cliente autenticado
    @GetMapping("/meus")
    public ResponseEntity<List<PedidoResponseDTO>> listarMeusPedidos(
            @RequestParam(required = false) CanalPedido canalPedido,
            Authentication authentication
    ) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pedidoService.listarMeusPedidos(
                        canalPedido,
                        authentication.getName()
                ));
    }

    // atualiza o status operacional de um pedido
    @PatchMapping("/{id}/status")
    public ResponseEntity<PedidoResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody PedidoStatusRequestDTO dto,
            Authentication authentication
    ) {

        // solicita a atualização do status ao serviço responsável
        PedidoResponseDTO pedido =
                pedidoService.atualizarStatus(
                        id,
                        dto,
                        authentication.getName()
                );

        // retorna os dados atualizados sem armazenamento em cache
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pedido);
    }

    // cancela um pedido que ainda aguarda pagamento
    @PatchMapping("/{id}/cancelamento")
    public ResponseEntity<PedidoResponseDTO> cancelar(
            @PathVariable Long id,
            Authentication authentication
    ) {

        // solicita o cancelamento ao serviço responsável
        PedidoResponseDTO pedido =
                pedidoService.cancelar(
                        id,
                        authentication.getName()
                );

        // retorna os dados do pedido cancelado
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pedido);
    }
}