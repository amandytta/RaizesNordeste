package br.com.raizesnordeste.raizesnordeste.enums;

// representa as possíveis situações de um pedido
// durante seu ciclo de atendimento
public enum StatusPedido {

    // pedido criado e aguardando a confirmação do pagamento
    AGUARDANDO_PAGAMENTO,

    // pedido recebido após a confirmação do pagamento
    RECEBIDO,

    // pedido em processo de preparação
    EM_PREPARO,

    // pedido preparado e disponível para entrega ou retirada
    PRONTO,

    // pedido finalizado após a entrega ao cliente
    CONCLUIDO,

    // pedido cancelado
    CANCELADO
}