package br.com.raizesnordeste.raizesnordeste.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

// representa um registro de auditoria das operações do sistema
@Entity
@Table(name = "auditoria")
public class Auditoria {

    // identificador único do evento de auditoria
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // usuário responsável pela operação, quando identificado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    // identifica a ação realizada no sistema
    @Column(nullable = false, length = 80, updatable = false)
    private String acao;

    // identifica a entidade afetada pela operação
    @Column(nullable = false, length = 80, updatable = false)
    private String entidade;

    // identificador do registro afetado pela operação
    @Column(name = "registro_id", updatable = false)
    private Long registroId;

    // armazena informações complementares em formato JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", updatable = false)
    private Map<String, Object> detalhes;

    // registra a data e a hora em que o evento ocorreu
    @Column(
            name = "data_evento",
            nullable = false,
            updatable = false
    )
    private LocalDateTime dataEvento;

    // construtor exigido pelo JPA
    public Auditoria() {
    }

    // define automaticamente a data de criação do registro
    @PrePersist
    protected void prePersist() {
        if (dataEvento == null) {
            dataEvento = LocalDateTime.now();
        }
    }

    // métodos de consulta dos atributos

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getAcao() {
        return acao;
    }

    public String getEntidade() {
        return entidade;
    }

    public Long getRegistroId() {
        return registroId;
    }

    public Map<String, Object> getDetalhes() {
        return detalhes;
    }

    public LocalDateTime getDataEvento() {
        return dataEvento;
    }

    // métodos utilizados na criação do registro de auditoria

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public void setEntidade(String entidade) {
        this.entidade = entidade;
    }

    public void setRegistroId(Long registroId) {
        this.registroId = registroId;
    }

    public void setDetalhes(Map<String, Object> detalhes) {
        this.detalhes = detalhes;
    }
}