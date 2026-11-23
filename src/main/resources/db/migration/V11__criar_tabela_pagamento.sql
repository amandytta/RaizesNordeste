CREATE TABLE pagamento (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           pedido_id BIGINT NOT NULL,
                           metodo VARCHAR(20) NOT NULL,
                           status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
                           valor DECIMAL(10,2) NOT NULL,
                           codigo_transacao VARCHAR(100) NOT NULL,
                           mensagem_retorno VARCHAR(255),
                           data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           data_processamento DATETIME NULL,

                           CONSTRAINT pk_pagamento
                               PRIMARY KEY (id),

                           CONSTRAINT fk_pagamento_pedido
                               FOREIGN KEY (pedido_id)
                                   REFERENCES pedido(id),

                           CONSTRAINT uk_pagamento_codigo_transacao
                               UNIQUE (codigo_transacao),

                           CONSTRAINT chk_pagamento_valor
                               CHECK (valor > 0),

                           CONSTRAINT chk_pagamento_metodo
                               CHECK (metodo IN (
                                                 'PIX',
                                                 'CREDITO',
                                                 'DEBITO'
                                   )),

                           CONSTRAINT chk_pagamento_status
                               CHECK (status IN (
                                                 'PENDENTE',
                                                 'APROVADO',
                                                 'RECUSADO'
                                   ))
);