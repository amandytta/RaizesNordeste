package br.com.raizesnordeste.raizesnordeste.entity;

import br.com.raizesnordeste.raizesnordeste.enums.CanalPedido;
import br.com.raizesnordeste.raizesnordeste.enums.FinalidadeConsentimento;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

// representa um registro de consentimento relacionado ao tratamento de dados
@Entity
@Table(name = "consentimento")
public class Consentimento {

    // identificador único do registro de consentimento
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // usuário responsável pela decisão registrada
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_id",
            nullable = false,
            updatable = false
    )
    private Usuario usuario;

    // finalidade específica do tratamento de dados
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(
            nullable = false,
            length = 100,
            updatable = false
    )
    private FinalidadeConsentimento finalidade;

    // versão do termo apresentado ao usuário
    @Column(
            name = "versao_termo",
            nullable = false,
            length = 30,
            updatable = false
    )
    private String versaoTermo;

    // indica se o consentimento foi concedido ou recusado
    @Column(nullable = false, updatable = false)
    private Boolean concedido;

    // data e horário em que a decisão foi registrada
    @Column(
            name = "data_registro",
            nullable = false,
            updatable = false
    )
    private LocalDateTime dataRegistro;

    // canal de atendimento utilizado para registrar a decisão
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(
            nullable = false,
            length = 20,
            updatable = false
    )
    private CanalPedido origem;

    // construtor padrão exigido pelo JPA
    public Consentimento() {
    }

    // define automaticamente a data de registro
    @PrePersist
    private void prePersist() {
        if (dataRegistro == null) {
            dataRegistro = LocalDateTime.now();
        }
    }

    // getters

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public FinalidadeConsentimento getFinalidade() {
        return finalidade;
    }

    public String getVersaoTermo() {
        return versaoTermo;
    }

    public Boolean getConcedido() {
        return concedido;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public CanalPedido getOrigem() {
        return origem;
    }

    // setters

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setFinalidade(
            FinalidadeConsentimento finalidade
    ) {
        this.finalidade = finalidade;
    }

    public void setVersaoTermo(String versaoTermo) {
        this.versaoTermo = versaoTermo;
    }

    public void setConcedido(Boolean concedido) {
        this.concedido = concedido;
    }

    public void setOrigem(CanalPedido origem) {
        this.origem = origem;
    }
}