CREATE TABLE usuario_unidade (
                                 usuario_id BIGINT NOT NULL,
                                 unidade_id BIGINT NOT NULL,
                                 ativo BOOLEAN NOT NULL DEFAULT TRUE,
                                 data_vinculo DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT pk_usuario_unidade
                                     PRIMARY KEY (usuario_id, unidade_id),

                                 CONSTRAINT fk_usuario_unidade_usuario
                                     FOREIGN KEY (usuario_id)
                                         REFERENCES usuario(id),

                                 CONSTRAINT fk_usuario_unidade_unidade
                                     FOREIGN KEY (unidade_id)
                                         REFERENCES unidade(id)
);