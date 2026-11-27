package br.com.raizesnordeste.raizesnordeste.entity;

import br.com.raizesnordeste.raizesnordeste.enums.CanalPedido;
import br.com.raizesnordeste.raizesnordeste.enums.StatusPedido;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// representa os pedidos realizados nas unidades da rede
@Entity
@Table(name = "pedido")
public class Pedido {

    // identificador único do pedido
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unidade responsável pelo atendimento do pedido
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    // usuário associado ao pedido
    // permite pedidos sem identificação de cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // canal pelo qual o pedido foi realizado
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalPedido canalPedido;

    // situação atual do pedido
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;

    // valor total calculado a partir dos itens do pedido
    @Column(
            name = "valor_total",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal valorTotal = BigDecimal.ZERO;

    // itens associados ao pedido
    // permite persistir os itens juntamente com o pedido
    @OneToMany(
            mappedBy = "pedido",
            cascade = CascadeType.PERSIST,
            fetch = FetchType.LAZY
    )
    private List<ItemPedido> itens = new ArrayList<>();

    // data e horário de criação do pedido
    @Column(
            name = "data_criacao",
            nullable = false,
            updatable = false
    )
    private LocalDateTime dataCriacao;

    // data e horário da última atualização do pedido
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    // construtor padrão exigido pelo JPA
    public Pedido() {
    }

    // define a data de criação antes da primeira persistência
    @PrePersist
    private void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    // atualiza a data sempre que o pedido for modificado
    @PreUpdate
    private void preUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    // adiciona um item e mantém os dois lados do relacionamento
    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        itens.add(item);
    }

    // getters

    public Long getId() {
        return id;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public CanalPedido getCanalPedido() {
        return canalPedido;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public List<ItemPedido> getItens() {
        return List.copyOf(itens);
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    // setters

    public void setUnidade(Unidade unidade) {
        this.unidade = unidade;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setCanalPedido(CanalPedido canalPedido) {
        this.canalPedido = canalPedido;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }
}