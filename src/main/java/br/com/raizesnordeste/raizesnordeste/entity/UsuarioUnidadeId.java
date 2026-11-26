package br.com.raizesnordeste.raizesnordeste.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

// representa a chave primária composta da tabela usuario_unidade
@Embeddable
public class UsuarioUnidadeId implements Serializable {

    // identificador da versão da classe para serialização
    private static final long serialVersionUID = 1L;

    // identificador do usuário que compõe a chave primária
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    // identificador da unidade que compõe a chave primária
    @Column(name = "unidade_id", nullable = false)
    private Long unidadeId;

    // construtor sem argumentos exigido pelo JPA
    public UsuarioUnidadeId() {
    }

    // construtor utilizado para criar uma chave composta
    public UsuarioUnidadeId(Long usuarioId, Long unidadeId) {
        this.usuarioId = usuarioId;
        this.unidadeId = unidadeId;
    }

    // retorna o identificador do usuário
    public Long getUsuarioId() {
        return usuarioId;
    }

    // retorna o identificador da unidade
    public Long getUnidadeId() {
        return unidadeId;
    }

    // compara duas chaves compostas pelos seus identificadores
    @Override
    public boolean equals(Object objeto) {

        // verifica se os objetos possuem a mesma referência
        if (this == objeto) {
            return true;
        }

        // verifica se o objeto pertence ao tipo esperado
        if (!(objeto instanceof UsuarioUnidadeId outro)) {
            return false;
        }

        // compara os dois identificadores da chave composta
        return Objects.equals(usuarioId, outro.usuarioId)
                && Objects.equals(unidadeId, outro.unidadeId);
    }

    // gera um código hash consistente com o método equals
    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, unidadeId);
    }
}