CREATE TABLE categoria (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           nome VARCHAR(80) NOT NULL,
                           descricao VARCHAR(255),
                           ativa BOOLEAN NOT NULL DEFAULT TRUE,
                           data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           data_atualizacao DATETIME NULL,

                           CONSTRAINT pk_categoria PRIMARY KEY (id),
                           CONSTRAINT uk_categoria_nome UNIQUE (nome)
);