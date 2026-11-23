CREATE TABLE usuario (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         nome VARCHAR(120) NOT NULL,
                         email VARCHAR(150) NOT NULL,
                         senha_hash VARCHAR(255) NOT NULL,
                         ativo BOOLEAN NOT NULL DEFAULT TRUE,
                         data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         data_atualizacao DATETIME NULL,

                         CONSTRAINT pk_usuario PRIMARY KEY (id),
                         CONSTRAINT uk_usuario_email UNIQUE (email)
);