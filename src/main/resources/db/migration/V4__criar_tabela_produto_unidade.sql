CREATE TABLE produto_unidade (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 produto_id BIGINT NOT NULL,
                                 unidade_id BIGINT NOT NULL,
                                 preco DECIMAL(10,2) NOT NULL,
                                 estoque INT NOT NULL DEFAULT 0,
                                 disponivel BOOLEAN NOT NULL DEFAULT TRUE,
                                 data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 data_atualizacao DATETIME NULL,

                                 CONSTRAINT pk_produto_unidade
                                     PRIMARY KEY (id),

                                 CONSTRAINT uk_produto_unidade
                                     UNIQUE (produto_id, unidade_id),

                                 CONSTRAINT fk_produto_unidade_produto
                                     FOREIGN KEY (produto_id)
                                         REFERENCES produto(id),

                                 CONSTRAINT fk_produto_unidade_unidade
                                     FOREIGN KEY (unidade_id)
                                         REFERENCES unidade(id),

                                 CONSTRAINT chk_produto_unidade_preco
                                     CHECK (preco > 0),

                                 CONSTRAINT chk_produto_unidade_estoque
                                     CHECK (estoque >= 0)
);