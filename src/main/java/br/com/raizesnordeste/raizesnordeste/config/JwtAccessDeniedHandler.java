package br.com.raizesnordeste.raizesnordeste.config;

import br.com.raizesnordeste.raizesnordeste.dto.ErroResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

// padroniza as respostas de erro para acessos sem permissao
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    // injeta o componente responsavel pela conversao de objetos em JSON
    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // executado quando o usuario autenticado nao possui permissao
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        // define o status HTTP 403
        response.setStatus(HttpStatus.FORBIDDEN.value());

        // configura o formato e a codificacao da resposta
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // impede o armazenamento da resposta em cache
        response.setHeader("Cache-Control", "no-store");

        // cria uma resposta padronizada de acesso negado
        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Acesso negado",
                "Você não possui permissão para acessar este recurso.",
                request.getRequestURI()
        );

        // converte o objeto de erro em JSON e envia a resposta
        objectMapper.writeValue(response.getOutputStream(), erro);
    }
}