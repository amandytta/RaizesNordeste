CREATE TABLE unidade (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         nome VARCHAR(100) NOT NULL,
                         cnpj VARCHAR(14) NOT NULL,
                         telefone VARCHAR(20),
                         email VARCHAR(150),
                         cep VARCHAR(8) NOT NULL,
                         logradouro VARCHAR(150) NOT NULL,
                         numero VARCHAR(20) NOT NULL,
                         complemento VARCHAR(100),
                         bairro VARCHAR(100) NOT NULL,
                         cidade VARCHAR(100) NOT NULL,
                         estado CHAR(2) NOT NULL,
                         ativa BOOLEAN NOT NULL DEFAULT TRUE,
                         data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         data_atualizacao DATETIME NULL,

                         CONSTRAINT pk_unidade PRIMARY KEY (id),
                         CONSTRAINT uk_unidade_cnpj UNIQUE (cnpj)
);