package br.com.raizesnordeste.raizesnordeste.dto;

import java.util.Set;

// representa os dados do usuario retornados pela API, sem expor senhas ou hashes de autenticação
public record UsuarioResponseDTO(

        // identificador unico do usuario
        Long id,

        // nome cadastrado do usuario
        String nome,

        // endereço de e-mail cadastrado
        String email,

        // indica se a conta esta ativa
        Boolean ativo,

        // contem os nomes dos perfis associados ao usuario
        Set<String> perfis

) {

    // evita a exposicao de dados pessoais caso o DTO seja convertido em texto para logs
    @Override
    public String toString() {
        return "UsuarioResponseDTO[dados protegidos]";
    }
}