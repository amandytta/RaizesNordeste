package br.com.raizesnordeste.raizesnordeste.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

// representa cada produto incluído em um pedido
@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    // identificador único do item
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // pedido ao qual o item pertence
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // associação entre o produto e a unidade responsável
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_unidade_id", nullable = false)
    private ProdutoUnidade produtoUnidade;

    // preserva o nome do produto no momento da compra
    @Column(name = "nome_produto", nullable = false, length = 120)
    private String nomeProduto;

    // quantidade solicitada pelo cliente
    @Column(nullable = false)
    private Integer quantidade;

    // preserva o preço unitário praticado no momento da compra
    @Column(
            name = "preco_unitario",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precoUnitario;

    // valor correspondente ao preço unitário multiplicado pela quantidade
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    // construtor padrão exigido pelo JPA
    public ItemPedido() {
    }

    // getters

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public ProdutoUnidade getProdutoUnidade() {
        return produtoUnidade;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    // setters

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public void setProdutoUnidade(ProdutoUnidade produtoUnidade) {
        this.produtoUnidade = produtoUnidade;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}