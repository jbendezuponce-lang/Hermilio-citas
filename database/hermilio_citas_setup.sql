-- ============================================================================
-- BASE DE DATOS COMPLETA - Hermilio Citas
-- Un solo script: crea la base, las 15 tablas y los datos iniciales.
-- Es seguro ejecutarlo varias veces (no borra datos existentes).
--
-- Uso:  mysql -u root -p < database/hermilio_citas_setup.sql
-- (o abrirlo en MySQL Workbench y pulsar el rayo "Execute")
--
-- Las cuentas de prueba (admin@, medico@, recepcion@, paciente@hermilio.com,
-- clave admin123) las crea el backend solo al arrancar.
-- ============================================================================
CREATE DATABASE IF NOT EXISTS hermilio_citas CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE hermilio_citas;

-- ============================================================================
-- Hospital Hermilio Valdizán - Sistema de gestión de citas
-- Modelo físico MySQL 8.0+ (InnoDB, utf8mb4)  -  15 tablas
--
-- Ejecución manual:
--   CREATE DATABASE hermilio_citas CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
--   mysql -u <usuario> -p hermilio_citas < schema.sql
--
-- Tablas intermedias N:M (3):
--   horarios_disponibles : medicos      N:M consultorios  (atributos: fecha, horas, turno, disponible)
--   citas                : pacientes    N:M horarios_disponibles (atributos: estado, origen, ...)
--   lista_espera         : pacientes    N:M especialidades (atributos: fecha_registro, estado)
-- El script es idempotente (CREATE TABLE IF NOT EXISTS).
-- ============================================================================

-- ---------------------------------------------------------------- Seguridad
CREATE TABLE IF NOT EXISTS roles (
    id_rol      BIGINT       NOT NULL AUTO_INCREMENT,
    nombre_rol  VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id_rol),
    CONSTRAINT uk_roles_nombre UNIQUE (nombre_rol),
    CONSTRAINT ck_roles_nombre CHECK (nombre_rol IN ('PACIENTE','RECEPCIONISTA','MEDICO','ADMINISTRADOR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario           BIGINT       NOT NULL AUTO_INCREMENT,
    id_rol               BIGINT       NOT NULL,
    documento_identidad  VARCHAR(15)  NOT NULL,
    nombres              VARCHAR(100) NOT NULL,
    apellidos            VARCHAR(100) NOT NULL,
    correo               VARCHAR(150) NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    estado               TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuarios_documento UNIQUE (documento_identidad),
    CONSTRAINT uk_usuarios_correo    UNIQUE (correo),
    INDEX idx_usuarios_rol (id_rol),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (id_rol) REFERENCES roles (id_rol)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------- Catálogos clínicos
CREATE TABLE IF NOT EXISTS especialidades (
    id_especialidad      BIGINT       NOT NULL AUTO_INCREMENT,
    nombre_especialidad  VARCHAR(80)  NOT NULL,
    descripcion          VARCHAR(255) NULL,
    PRIMARY KEY (id_especialidad),
    CONSTRAINT uk_especialidades_nombre UNIQUE (nombre_especialidad)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consultorios (
    id_consultorio   BIGINT      NOT NULL AUTO_INCREMENT,
    numero_pabellon  VARCHAR(20) NOT NULL,
    codigo_room      VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_consultorio),
    CONSTRAINT uk_consultorios_ubicacion UNIQUE (numero_pabellon, codigo_room)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------- Perfiles de usuario (1:1)
CREATE TABLE IF NOT EXISTS pacientes (
    id_paciente        BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario         BIGINT       NOT NULL,
    tipo_seguro        VARCHAR(50)  NULL,
    telefono_contacto  VARCHAR(20)  NULL,
    datos_apoderado    VARCHAR(255) NULL,
    PRIMARY KEY (id_paciente),
    CONSTRAINT uk_pacientes_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_pacientes_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS medicos (
    id_medico           BIGINT      NOT NULL AUTO_INCREMENT,
    id_usuario          BIGINT      NOT NULL,
    id_especialidad     BIGINT      NOT NULL,
    numero_colegiatura  VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_medico),
    CONSTRAINT uk_medicos_usuario      UNIQUE (id_usuario),
    CONSTRAINT uk_medicos_colegiatura  UNIQUE (numero_colegiatura),
    INDEX idx_medicos_especialidad (id_especialidad),
    CONSTRAINT fk_medicos_usuario      FOREIGN KEY (id_usuario)      REFERENCES usuarios (id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_medicos_especialidad FOREIGN KEY (id_especialidad) REFERENCES especialidades (id_especialidad)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS administrativos (
    id_administrativo  BIGINT      NOT NULL AUTO_INCREMENT,
    id_usuario         BIGINT      NOT NULL,
    cargo              VARCHAR(80) NOT NULL,
    PRIMARY KEY (id_administrativo),
    CONSTRAINT uk_administrativos_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_administrativos_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------- INTERMEDIA 1: medicos N:M consultorios
CREATE TABLE IF NOT EXISTS horarios_disponibles (
    id_horario      BIGINT      NOT NULL AUTO_INCREMENT,
    id_medico       BIGINT      NOT NULL,
    id_consultorio  BIGINT      NOT NULL,
    fecha           DATE        NOT NULL,
    hora_inicio     TIME        NOT NULL,
    hora_fin        TIME        NOT NULL,
    turno           VARCHAR(10) NOT NULL,
    disponible      TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (id_horario),
    CONSTRAINT uk_horario_medico       UNIQUE (id_medico, fecha, hora_inicio),
    CONSTRAINT uk_horario_consultorio  UNIQUE (id_consultorio, fecha, hora_inicio),
    INDEX idx_horarios_busqueda (fecha, disponible),
    CONSTRAINT ck_horarios_horas CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_horarios_turno CHECK (turno IN ('MANANA','TARDE')),
    CONSTRAINT fk_horarios_medico      FOREIGN KEY (id_medico)      REFERENCES medicos (id_medico)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_horarios_consultorio FOREIGN KEY (id_consultorio) REFERENCES consultorios (id_consultorio)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------- INTERMEDIA 2: pacientes N:M horarios_disponibles
CREATE TABLE IF NOT EXISTS citas (
    id_cita             BIGINT      NOT NULL AUTO_INCREMENT,
    id_paciente         BIGINT      NOT NULL,
    id_horario          BIGINT      NOT NULL,
    fecha_hora_reserva  DATETIME    NOT NULL,
    estado              VARCHAR(15) NOT NULL,
    origen              VARCHAR(10) NOT NULL,
    reprogramaciones    INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id_cita),
    INDEX idx_citas_paciente (id_paciente),
    INDEX idx_citas_horario  (id_horario, estado),
    INDEX idx_citas_estado   (estado),
    CONSTRAINT ck_citas_estado CHECK (estado IN ('CONFIRMADA','REPROGRAMADA','CHECK_IN','ATENDIDA','CANCELADA','NO_ASISTIO')),
    CONSTRAINT ck_citas_origen CHECK (origen IN ('MOVIL','PRESENCIAL')),
    CONSTRAINT ck_citas_reprog CHECK (reprogramaciones >= 0),
    CONSTRAINT fk_citas_paciente FOREIGN KEY (id_paciente) REFERENCES pacientes (id_paciente)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_citas_horario  FOREIGN KEY (id_horario)  REFERENCES horarios_disponibles (id_horario)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------- INTERMEDIA 3: pacientes N:M especialidades (RF-24)
CREATE TABLE IF NOT EXISTS lista_espera (
    id_lista_espera  BIGINT      NOT NULL AUTO_INCREMENT,
    id_paciente      BIGINT      NOT NULL,
    id_especialidad  BIGINT      NOT NULL,
    fecha_registro   DATETIME    NOT NULL,
    estado           VARCHAR(12) NOT NULL,
    PRIMARY KEY (id_lista_espera),
    INDEX idx_espera_especialidad (id_especialidad, estado),
    INDEX idx_espera_paciente     (id_paciente),
    CONSTRAINT ck_espera_estado CHECK (estado IN ('EN_ESPERA','ATENDIDA')),
    CONSTRAINT fk_espera_paciente     FOREIGN KEY (id_paciente)     REFERENCES pacientes (id_paciente)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_espera_especialidad FOREIGN KEY (id_especialidad) REFERENCES especialidades (id_especialidad)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------- Atención clínica (1:N)
CREATE TABLE IF NOT EXISTS triajes (
    id_triaje         BIGINT       NOT NULL AUTO_INCREMENT,
    id_cita           BIGINT       NOT NULL,
    presion_arterial  VARCHAR(10)  NULL,
    peso              DECIMAL(5,2) NULL,
    signos_vitales    VARCHAR(255) NULL,
    fecha_registro    DATETIME     NOT NULL,
    PRIMARY KEY (id_triaje),
    CONSTRAINT uk_triajes_cita UNIQUE (id_cita),
    CONSTRAINT fk_triajes_cita FOREIGN KEY (id_cita) REFERENCES citas (id_cita)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS historiales_clinicos (
    id_historial    BIGINT   NOT NULL AUTO_INCREMENT,
    id_cita         BIGINT   NOT NULL,
    diagnostico     TEXT     NULL,
    tratamiento     TEXT     NOT NULL,
    observaciones   TEXT     NULL,
    fecha_registro  DATETIME NOT NULL,
    PRIMARY KEY (id_historial),
    CONSTRAINT uk_historial_cita UNIQUE (id_cita),
    CONSTRAINT fk_historial_cita FOREIGN KEY (id_cita) REFERENCES citas (id_cita)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recetas_medicas (
    id_receta     BIGINT       NOT NULL AUTO_INCREMENT,
    id_historial  BIGINT       NOT NULL,
    medicamento   VARCHAR(150) NOT NULL,
    dosis         VARCHAR(100) NOT NULL,
    indicaciones  VARCHAR(500) NULL,
    PRIMARY KEY (id_receta),
    INDEX idx_recetas_historial (id_historial),
    CONSTRAINT fk_recetas_historial FOREIGN KEY (id_historial) REFERENCES historiales_clinicos (id_historial)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------- Notificaciones y trazabilidad
CREATE TABLE IF NOT EXISTS notificaciones (
    id_notificacion  BIGINT       NOT NULL AUTO_INCREMENT,
    id_cita          BIGINT       NOT NULL,
    tipo             VARCHAR(15)  NOT NULL,
    mensaje          VARCHAR(500) NOT NULL,
    fecha_envio      DATETIME     NOT NULL,
    leido            TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id_notificacion),
    INDEX idx_notificaciones_cita (id_cita, tipo),
    CONSTRAINT ck_notificaciones_tipo CHECK (tipo IN ('CONFIRMACION','RECORDATORIO','REPROGRAMACION','CANCELACION')),
    CONSTRAINT fk_notificaciones_cita FOREIGN KEY (id_cita) REFERENCES citas (id_cita)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auditoria_logs (
    id_log      BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario  BIGINT       NULL,
    accion      VARCHAR(300) NOT NULL,
    fecha_hora  DATETIME     NOT NULL,
    PRIMARY KEY (id_log),
    INDEX idx_auditoria_fecha   (fecha_hora),
    INDEX idx_auditoria_usuario (id_usuario),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------ Datos iniciales
-- Datos iniciales mínimos (idempotentes). No incluye usuarios ni contraseñas:
-- el primer ADMINISTRADOR se crea al arrancar con ADMIN_DOCUMENTO / ADMIN_EMAIL / ADMIN_PASSWORD.

INSERT IGNORE INTO roles (nombre_rol) VALUES
    ('PACIENTE'), ('RECEPCIONISTA'), ('MEDICO'), ('ADMINISTRADOR');

-- Alcance del proyecto: consultorios externos de psiquiatría y psicología (RF-05)
INSERT IGNORE INTO especialidades (nombre_especialidad, descripcion) VALUES
    ('Psiquiatría', 'Consulta externa de psiquiatría'),
    ('Psicología',  'Consulta externa de psicología');
