package br.com.raizesnordeste.raizesnordeste.exception;

import br.com.raizesnordeste.raizesnordeste.dto.ErroResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErroResponseDTO> tratarNaoEncontrado(
            NoSuchElementException ex,
            HttpServletRequest request) {

        return criarResposta(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponseDTO> tratarArgumentoInvalido(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return criarResposta(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> tratarValidacao(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(erro -> erro.getField() + ": "
                        + erro.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        return criarResposta(
                HttpStatus.BAD_REQUEST,
                mensagem,
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponseDTO> tratarIntegridade(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        return criarResposta(
                HttpStatus.CONFLICT,
                "A operação viola uma restrição dos dados.",
                request
        );
    }

    private ResponseEntity<ErroResponseDTO> criarResposta(
            HttpStatus status,
            String mensagem,
            HttpServletRequest request) {

        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(erro);
    }

    // trata falhas de autenticacao sem revelar informacoes sobre a conta
    @ExceptionHandler({
            BadCredentialsException.class,
            AccountStatusException.class,
            UsernameNotFoundException.class
    })
    public ResponseEntity<ErroResponseDTO> tratarFalhaAutenticacao(
            HttpServletRequest request) {

        // define o status HTTP 401 para credenciais invalidas
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        // cria uma resposta de erro padronizada e segura
        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(),
                status.value(),
                "Não autorizado",
                "Credenciais inválidas.",
                request.getRequestURI()
        );

        // retorna a resposta sem expor detalhes internos da autenticacao
        return ResponseEntity.status(status).body(erro);
    }

    // trata erros de validacao identificados na camada de serviço
    // retorna HTTP 400 sem alterar o tratamento dos conflitos
    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroResponseDTO> tratarDadosInvalidos(
            DadosInvalidosException ex,
            HttpServletRequest request
    ) {

        // monta a resposta seguindo o padrão de erros da API
        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Requisição inválida",
                ex.getMessage(),
                request.getRequestURI()
        );

        // retorna HTTP 400 com os detalhes do erro
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(erro);
    }
}