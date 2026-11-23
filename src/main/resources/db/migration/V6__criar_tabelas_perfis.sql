CREATE TABLE perfil (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        nome VARCHAR(30) NOT NULL,
                        descricao VARCHAR(150),

                        CONSTRAINT pk_perfil PRIMARY KEY (id),
                        CONSTRAINT uk_perfil_nome UNIQUE (nome)
);

CREATE TABLE usuario_perfil (
                                usuario_id BIGINT NOT NULL,
                                perfil_id BIGINT NOT NULL,

                                CONSTRAINT pk_usuario_perfil
                                    PRIMARY KEY (usuario_id, perfil_id),

                                CONSTRAINT fk_usuario_perfil_usuario
                                    FOREIGN KEY (usuario_id)
                                        REFERENCES usuario(id),

                                CONSTRAINT fk_usuario_perfil_perfil
                                    FOREIGN KEY (perfil_id)
                                        REFERENCES perfil(id)
);