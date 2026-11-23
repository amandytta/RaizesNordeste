CREATE TABLE item_pedido (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             pedido_id BIGINT NOT NULL,
                             produto_unidade_id BIGINT NOT NULL,
                             nome_produto VARCHAR(120) NOT NULL,
                             quantidade INT NOT NULL,
                             preco_unitario DECIMAL(10,2) NOT NULL,
                             subtotal DECIMAL(10,2) NOT NULL,

                             CONSTRAINT pk_item_pedido
                                 PRIMARY KEY (id),

                             CONSTRAINT fk_item_pedido_pedido
                                 FOREIGN KEY (pedido_id)
                                     REFERENCES pedido(id),

                             CONSTRAINT fk_item_pedido_produto_unidade
                                 FOREIGN KEY (produto_unidade_id)
                                     REFERENCES produto_unidade(id),

                             CONSTRAINT chk_item_pedido_quantidade
                                 CHECK (quantidade > 0),

                             CONSTRAINT chk_item_pedido_preco
                                 CHECK (preco_unitario > 0),

                             CONSTRAINT chk_item_pedido_subtotal
                                 CHECK (subtotal >= 0)
);