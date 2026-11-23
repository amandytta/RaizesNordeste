CREATE TABLE auditoria (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           usuario_id BIGINT NULL,
                           acao VARCHAR(80) NOT NULL,
                           entidade VARCHAR(80) NOT NULL,
                           registro_id BIGINT NULL,
                           detalhes JSON NULL,
                           data_evento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT pk_auditoria
                               PRIMARY KEY (id),

                           CONSTRAINT fk_auditoria_usuario
                               FOREIGN KEY (usuario_id)
                                   REFERENCES usuario(id)
);