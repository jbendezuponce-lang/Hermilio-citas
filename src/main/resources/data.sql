-- Datos iniciales mínimos (idempotentes). No incluye usuarios ni contraseñas:
-- el primer ADMINISTRADOR se crea al arrancar con ADMIN_DOCUMENTO / ADMIN_EMAIL / ADMIN_PASSWORD.

INSERT IGNORE INTO roles (nombre_rol) VALUES
    ('PACIENTE'), ('RECEPCIONISTA'), ('MEDICO'), ('ADMINISTRADOR');

-- Alcance del proyecto: consultorios externos de psiquiatría y psicología (RF-05)
INSERT IGNORE INTO especialidades (nombre_especialidad, descripcion) VALUES
    ('Psiquiatría', 'Consulta externa de psiquiatría'),
    ('Psicología',  'Consulta externa de psicología');
