CREATE TABLE consentimento (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               usuario_id BIGINT NOT NULL,
                               finalidade VARCHAR(100) NOT NULL,
                               versao_termo VARCHAR(30) NOT NULL,
                               concedido BOOLEAN NOT NULL,
                               data_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               origem VARCHAR(20) NOT NULL,

                               CONSTRAINT pk_consentimento
                                   PRIMARY KEY (id),

                               CONSTRAINT fk_consentimento_usuario
                                   FOREIGN KEY (usuario_id)
                                       REFERENCES usuario(id),

                               CONSTRAINT chk_consentimento_origem
                                   CHECK (origem IN (
                                                     'APP',
                                                     'TOTEM',
                                                     'BALCAO',
                                                     'PICKUP',
                                                     'WEB'
                                       ))
);