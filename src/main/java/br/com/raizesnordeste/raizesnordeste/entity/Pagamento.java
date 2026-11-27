package br.com.raizesnordeste.raizesnordeste.entity;

import br.com.raizesnordeste.raizesnordeste.enums.MetodoPagamento;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPagamento;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// representa uma tentativa de pagamento associada a um pedido
@Entity
@Table(name = "pagamento")
public class Pagamento {

    // identificador único do pagamento
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // pedido ao qual o pagamento pertence
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // método escolhido para realizar o pagamento
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private MetodoPagamento metodo;

    // situação atual da transação
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    // valor da transação determinado pelo backend
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    // código único utilizado para identificar a transação
    @Column(
            name = "codigo_transacao",
            nullable = false,
            unique = true,
            length = 100,
            updatable = false
    )
    private String codigoTransacao;

    // mensagem retornada pelo gateway de pagamento
    @Column(name = "mensagem_retorno", length = 255)
    private String mensagemRetorno;

    // data e horário de criação da tentativa de pagamento
    @Column(
            name = "data_criacao",
            nullable = false,
            updatable = false
    )
    private LocalDateTime dataCriacao;

    // data e horário em que o pagamento foi processado
    @Column(name = "data_processamento")
    private LocalDateTime dataProcessamento;

    // construtor padrão exigido pelo JPA
    public Pagamento() {
    }

    // define a data de criação antes da primeira persistência
    @PrePersist
    private void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    // getters

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public MetodoPagamento getMetodo() {
        return metodo;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getCodigoTransacao() {
        return codigoTransacao;
    }

    public String getMensagemRetorno() {
        return mensagemRetorno;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataProcessamento() {
        return dataProcessamento;
    }

    // setters

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public void setMetodo(MetodoPagamento metodo) {
        this.metodo = metodo;
    }

    public void setStatus(StatusPagamento status) {
        this.status = status;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public void setCodigoTransacao(String codigoTransacao) {
        this.codigoTransacao = codigoTransacao;
    }

    public void setMensagemRetorno(String mensagemRetorno) {
        this.mensagemRetorno = mensagemRetorno;
    }

    public void setDataProcessamento(
            LocalDateTime dataProcessamento
    ) {
        this.dataProcessamento = dataProcessamento;
    }

    // define o momento em que a tentativa de pagamento foi iniciada
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}