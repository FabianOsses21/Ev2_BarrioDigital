-- Esquema CATALOG (bd_catalog). Los nombres siguen la estrategia
-- física de Spring Boot (camelCase -> snake_case), requerido por
-- spring.jpa.hibernate.ddl-auto=validate.

CREATE TABLE tipos_tramite (
    id           VARCHAR2(64 CHAR)  NOT NULL,
    nombre       VARCHAR2(120 CHAR) NOT NULL,
    descripcion  VARCHAR2(500 CHAR) NOT NULL,
    cupo_diario  NUMBER(10,0)       NOT NULL,
    activo       NUMBER(1,0)        NOT NULL,
    CONSTRAINT pk_tipos_tramite PRIMARY KEY (id)
);

-- El catálogo se siembra solo al primer arranque (CommandLineRunner
-- inserta los 3 tipos si la tabla está vacía). No agregar seeds aquí.
