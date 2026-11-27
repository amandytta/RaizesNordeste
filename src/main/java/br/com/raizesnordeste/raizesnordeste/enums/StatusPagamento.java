package br.com.raizesnordeste.raizesnordeste.enums;

// representa as possíveis situações de uma transação de pagamento
public enum StatusPagamento {

    // pagamento criado e aguardando processamento
    PENDENTE,

    // pagamento processado e aprovado pelo gateway
    APROVADO,

    // pagamento processado e recusado pelo gateway
    RECUSADO
}