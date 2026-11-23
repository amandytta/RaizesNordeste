CREATE TABLE movimentacao_estoque (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      produto_unidade_id BIGINT NOT NULL,
                                      pedido_id BIGINT NULL,
                                      usuario_id BIGINT NULL,
                                      tipo VARCHAR(20) NOT NULL,
                                      quantidade INT NOT NULL,
                                      estoque_anterior INT NOT NULL,
                                      estoque_posterior INT NOT NULL,
                                      motivo VARCHAR(255),
                                      data_movimentacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT pk_movimentacao_estoque
                                          PRIMARY KEY (id),

                                      CONSTRAINT fk_movimentacao_produto_unidade
                                          FOREIGN KEY (produto_unidade_id)
                                              REFERENCES produto_unidade(id),

                                      CONSTRAINT fk_movimentacao_pedido
                                          FOREIGN KEY (pedido_id)
                                              REFERENCES pedido(id),

                                      CONSTRAINT fk_movimentacao_usuario
                                          FOREIGN KEY (usuario_id)
                                              REFERENCES usuario(id),

                                      CONSTRAINT chk_movimentacao_tipo
                                          CHECK (tipo IN ('ENTRADA', 'SAIDA', 'AJUSTE')),

                                      CONSTRAINT chk_movimentacao_quantidade
                                          CHECK (quantidade > 0),

                                      CONSTRAINT chk_movimentacao_estoque_anterior
                                          CHECK (estoque_anterior >= 0),

                                      CONSTRAINT chk_movimentacao_estoque_posterior
                                          CHECK (estoque_posterior >= 0)
);