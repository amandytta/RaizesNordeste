package br.com.raizesnordeste.raizesnordeste.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import br.com.raizesnordeste.raizesnordeste.dto.ErroResponseDTO;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

// padroniza as respostas de erro para requisicoes nao autenticadas
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    // injeta o componente responsavel pela conversao de objetos em JSON
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // executado quando uma requisicao nao possui autenticacao valida
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        // define o status HTTP 401
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        // configura o formato e a codificacao da resposta
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // informa o mecanismo de autenticacao utilizado pela API
        response.setHeader("WWW-Authenticate", "Bearer");

        // impede o armazenamento da resposta em cache
        response.setHeader("Cache-Control", "no-store");

        // cria uma resposta padronizada sem expor detalhes internos
        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Não autorizado",
                "Token ausente, inválido ou expirado.",
                request.getRequestURI()
        );

        // converte o objeto de erro em JSON e envia a resposta
        objectMapper.writeValue(response.getOutputStream(), erro);
    }
}