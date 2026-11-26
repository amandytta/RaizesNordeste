package br.com.raizesnordeste.raizesnordeste.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// representa o vínculo entre um usuário e uma unidade da rede
@Entity
@Table(name = "usuario_unidade")
public class UsuarioUnidade {

    // chave primária composta pelos identificadores do usuário e da unidade
    @EmbeddedId
    private UsuarioUnidadeId id;

    // relacionamento com o usuário associado ao vínculo
    @MapsId("usuarioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // relacionamento com a unidade associada ao vínculo
    @MapsId("unidadeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    // indica se o vínculo está ativo
    @Column(nullable = false)
    private Boolean ativo = true;

    // armazena a data de criação do vínculo
    @Column(name = "data_vinculo", nullable = false, updatable = false)
    private LocalDateTime dataVinculo;

    // construtor sem argumentos exigido pelo JPA
    public UsuarioUnidade() {
    }

    // construtor utilizado para estabelecer um novo vínculo
    public UsuarioUnidade(Usuario usuario, Unidade unidade) {
        this.usuario = usuario;
        this.unidade = unidade;

        // inicializa a chave composta com os identificadores existentes
        this.id = new UsuarioUnidadeId(
                usuario.getId(),
                unidade.getId()
        );
    }

    // define a data de criação antes da persistência do vínculo
    @PrePersist
    public void prePersist() {
        if (dataVinculo == null) {
            dataVinculo = LocalDateTime.now();
        }
    }

    // retorna a chave primária composta
    public UsuarioUnidadeId getId() {
        return id;
    }

    // retorna o usuário vinculado
    public Usuario getUsuario() {
        return usuario;
    }

    // retorna a unidade vinculada
    public Unidade getUnidade() {
        return unidade;
    }

    // retorna a situação atual do vínculo
    public Boolean getAtivo() {
        return ativo;
    }

    // permite ativar ou desativar o vínculo
    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    // retorna a data de criação do vínculo
    public LocalDateTime getDataVinculo() {
        return dataVinculo;
    }
}