CREATE TABLE pedido (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        unidade_id BIGINT NOT NULL,
                        usuario_id BIGINT NULL,
                        canal VARCHAR(20) NOT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',
                        valor_total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                        data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        data_atualizacao DATETIME NULL,

                        CONSTRAINT pk_pedido
                            PRIMARY KEY (id),

                        CONSTRAINT fk_pedido_unidade
                            FOREIGN KEY (unidade_id)
                                REFERENCES unidade(id),

                        CONSTRAINT fk_pedido_usuario
                            FOREIGN KEY (usuario_id)
                                REFERENCES usuario(id),

                        CONSTRAINT chk_pedido_valor_total
                            CHECK (valor_total >= 0),

                        CONSTRAINT chk_pedido_canal
                            CHECK (canal IN (
                                             'APP',
                                             'TOTEM',
                                             'BALCAO',
                                             'PICKUP',
                                             'WEB'
                                )),

                        CONSTRAINT chk_pedido_status
                            CHECK (status IN (
                                              'AGUARDANDO_PAGAMENTO',
                                              'RECEBIDO',
                                              'EM_PREPARO',
                                              'PRONTO',
                                              'CONCLUIDO',
                                              'CANCELADO'
                                ))
);