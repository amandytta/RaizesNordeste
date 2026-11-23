CREATE TABLE produto (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         categoria_id BIGINT NOT NULL,
                         nome VARCHAR(120) NOT NULL,
                         descricao VARCHAR(500),
                         imagem_url VARCHAR(500),
                         ativo BOOLEAN NOT NULL DEFAULT TRUE,
                         data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         data_atualizacao DATETIME NULL,

                         CONSTRAINT pk_produto PRIMARY KEY (id),

                         CONSTRAINT fk_produto_categoria
                             FOREIGN KEY (categoria_id)
                                 REFERENCES categoria(id)
);