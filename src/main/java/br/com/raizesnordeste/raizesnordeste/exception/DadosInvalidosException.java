package br.com.raizesnordeste.raizesnordeste.exception;

// representa erros relacionados a dados inválidos identificados durante a execução das regras de negócio
public class DadosInvalidosException extends RuntimeException {

    // recebe a mensagem que descreve o motivo da rejeição
    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}