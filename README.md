# Backend de gestión de citas – Hospital Hermilio Valdizán (Integrador I, UTP)

API REST del sistema web y móvil de citas de consultorios externos de **Psiquiatría y Psicología**.
Todo el alcance sale del documento `Integrador_S07.docx`; el análisis y las decisiones de diseño están en
[`docs/ANALISIS_Y_DISENO.md`](docs/ANALISIS_Y_DISENO.md).

## Estado de verificación (léase primero)
- El código fue escrito y **comprobado sintácticamente/de tipos con `javac` contra stubs** de las librerías.
- **No se ejecutó** `mvn` (Maven Central no estaba accesible en el entorno de desarrollo), por lo que **las pruebas no se han ejecutado** ni la aplicación se ha arrancado contra MySQL. La primera vez, ejecute `mvn clean verify` y revise el resultado.

## Stack
Java 21 · Spring Boot 3.5.6 · Spring MVC · Spring Data JPA/Hibernate · Spring Security (JWT sin estado) · Bean Validation · MySQL 8 · Maven · JUnit 5 + MockMvc + H2 (solo pruebas).

## Arquitectura (RNF-04, RNF-05)
MVC por capas en `pe.utp.hermilio.citas`:

| Paquete | Contenido |
|---|---|
| `controller` | Endpoints REST, validación de entrada, `@PreAuthorize` por rol |
| `service` | Reglas de negocio y transacciones |
| `repository` | Spring Data JPA (consultas con `JOIN FETCH`, bloqueo pesimista de horarios) |
| `model.entity` / `model.enums` | 15 entidades JPA (todas las relaciones `LAZY`, sin `CascadeType.ALL` salvo Historial→Recetas) |
| `dto` | Records de entrada/salida agrupados por módulo + `DtoMapper` |
| `exception` | Excepciones de negocio + `GlobalExceptionHandler` (`@RestControllerAdvice`) |
| `security`, `config` | JWT, filtro, `SecurityConfig`, propiedades, reloj (`Clock`, America/Lima), administrador inicial |

## Modelo de datos
15 tablas; las 3 clases intermedias que resuelven N:M son `horarios_disponibles` (Médico×Consultorio), `citas` (Paciente×Horario) y `lista_espera` (Paciente×Especialidad). Diagramas Mermaid en `docs/ANALISIS_Y_DISENO.md`; DDL en `src/main/resources/schema.sql`, datos base en `data.sql`.

## Puesta en marcha

### 1. Base de datos MySQL
```sql
CREATE DATABASE hermilio_citas CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'hermilio_app'@'%' IDENTIFIED BY '<contraseña-segura>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON hermilio_citas.* TO 'hermilio_app'@'%';
```
Cree las tablas ejecutando `schema.sql` y `data.sql` con el cliente `mysql`, **o** arranque una vez con `SQL_INIT_MODE=always` (los scripts son idempotentes).

### 2. Variables de entorno
| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_PASSWORD` | sí | Contraseña de MySQL (sin valor por defecto) |
| `JWT_SECRET` | sí | Secreto de firma, **mínimo 32 caracteres** |
| `DB_URL` | no | Por defecto `jdbc:mysql://localhost:3306/hermilio_citas` |
| `DB_USERNAME` | no | Por defecto `hermilio_app` |
| `JWT_EXPIRATION_MINUTES` | no | Por defecto 60 |
| `JPA_DDL_AUTO` | no | `none` (defecto) o `validate` |
| `SQL_INIT_MODE` | no | `never` (defecto) o `always` |
| `APP_TIMEZONE` | no | Por defecto `America/Lima` |
| `RECORDATORIO_CRON` | no | Por defecto `0 0 8 * * *` (recordatorio de citas de mañana, RF-12) |
| `ADMIN_DOCUMENTO`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | no | Si se definen las tres y no existe administrador, se crea el primero (RF-31) |
| `SEED_DEMO_USERS` | no | `true` (defecto, solo desarrollo): al arrancar crea/repara 4 cuentas de prueba (`admin@`, `medico@`, `recepcion@`, `paciente@hermilio.com`). **Poner `false` en producción** |
| `DEMO_PASSWORD` | no | Contraseña de las cuentas demo. Por defecto `admin123` (se guarda siempre cifrada con BCrypt) |

### 3. Ejecutar
```bash
export DB_PASSWORD='...' JWT_SECRET='...(32+ caracteres)...'
export ADMIN_DOCUMENTO=12345678 ADMIN_EMAIL=admin@hospital.local ADMIN_PASSWORD='...'
mvn spring-boot:run
```

### 4. Pruebas
```bash
mvn test
```
Usan H2 en modo MySQL (`src/test/resources/application.properties`). Cubren: relaciones JPA e integridad (5), reglas de citas (13) y seguridad/contratos HTTP (14).

## Endpoints
Todos bajo `/api`, JSON, `Authorization: Bearer <token>` salvo los públicos. Errores con formato `ApiError` (`timestamp, status, error, message, path, detalles`).

| Recurso | Endpoints (rol) |
|---|---|
| Auth (público) | `POST /auth/login` 200/400/401 · `POST /auth/registro` 201/400/409 · `POST /auth/logout` 204 (autenticado) · `POST /auth/recuperar-password` 202 · `POST /auth/restablecer-password` 200/400 |
| Pacientes | `GET/PUT /pacientes/me` (PACIENTE) · `POST /pacientes` (RECEPCIONISTA) · `GET /pacientes?documento=` (RECEP., MÉDICO, ADMIN) |
| Horarios | `GET /horarios?especialidadId[&fecha&turno]` (PACIENTE/RECEP./ADMIN) · `GET /horarios/medico/{id}` · `POST` `DELETE /horarios/{id}` (ADMIN) · `PUT /horarios/{id}` (RECEP./ADMIN) |
| Citas | `POST /citas` (PACIENTE) · `POST /citas/presencial` (RECEP.) · `GET /citas/mias` · `GET /citas/agenda` (RECEP./ADMIN) · `GET /citas/agenda/mia` (MÉDICO) · `GET /citas/{id}` · `PUT /citas/{id}/reprogramar` · `PATCH /citas/{id}/cancelar` · `PATCH .../check-in` (RECEP.) · `PATCH .../atender` (MÉDICO) · `PATCH .../no-asistio` (RECEP./MÉDICO) |
| Lista de espera (RECEP.) | `POST` · `GET` · `PATCH /lista-espera/{id}/atender` · `DELETE /lista-espera/{id}` |
| Triaje | `POST /triajes` · `GET /triajes/cita/{idCita}` (RECEP./MÉDICO) |
| Historial | `POST /historiales` · `PUT /historiales/{id}` · `GET /historiales/paciente/{idPaciente}` (MÉDICO) · `GET /historiales/mios` (PACIENTE, solo lectura) |
| Notificaciones (PACIENTE) | `GET /notificaciones/mias` · `PATCH /notificaciones/{id}/leida` |
| Catálogos | `GET /especialidades` y `GET /medicos` (**públicos**, alimentan las páginas públicas) · `POST/PUT/DELETE /especialidades[/{id}]` (ADMIN) · `GET/POST/PUT /consultorios` |
| Usuarios (ADMIN) | `GET/POST /usuarios` · `GET/PUT /usuarios/{id}` · `PATCH /usuarios/{id}/estado` |
| Reportes (ADMIN) | `GET /reportes/citas?desde&hasta` · `GET /reportes/inasistencia?desde&hasta[&especialidadId]` · `GET /auditoria?desde&hasta[&usuarioId]` |

Códigos habituales: 400 validación/regla de negocio · 401 sin token o token inválido · 403 rol o propiedad · 404 no existe · 409 conflicto (horario ocupado, cita duplicada RF-35, estado inválido) · 500 error inesperado (sin detalles internos).

## Frontend web (carpeta `medical-system/`)
Todas las pantallas leen y escriben **en MySQL a través de esta API** (`js/api.js` es el cliente REST con JWT). Ya no se usa `localStorage` para guardar datos de negocio; solo guarda el token de sesión.

**Cómo usarlo**
1. Arranque el backend (`mvn clean spring-boot:run`, puerto 8080).
2. Abra `medical-system/index.html` con la extensión *Live Server* de VS Code, o ejecute `python -m http.server 5500` dentro de `medical-system/` y abra `http://localhost:5500`. (Abrirlo con doble clic, como `file://`, también funciona porque CORS está habilitado.)
3. Si el backend corre en otra dirección, ejecute en la consola del navegador `localStorage.setItem('apiBase','http://HOST:PUERTO/api')`.

**Cuentas demo** (se crean/reparan al arrancar mientras `SEED_DEMO_USERS=true`; contraseña `admin123`): `admin@`, `medico@`, `recepcion@`, `paciente@hermilio.com`. Con las cuentas demo se siembran además 2 consultorios y franjas de 30 min del médico demo para los próximos 14 días hábiles, de modo que se puede reservar una cita desde el primer arranque.

**Qué hace cada rol**
| Rol | Pantallas conectadas |
|---|---|
| Administrador | Usuarios, Doctores, Pacientes, Especialidades, Ubicaciones (consultorios), **Schedules** (genera franjas de atención por médico/fecha), Reportes (citas y absentismo, CSV), Auditoría, Dashboard |
| Paciente | Reservar (especialidad → fecha → hora), Mis citas (ver, reprogramar, cancelar), Historial clínico, Recetas, Notificaciones, Perfil |
| Recepción | Agenda diaria, cita presencial, reprogramar/cancelar presenciales, check-in, no-asistió, lista de espera, registro y búsqueda de pacientes |
| Médico | Agenda, consulta (triaje + historial + recetas + marcar atendida), historial por paciente (editable), horario semanal |
| Público | Inicio, Especialidades, Médicos y perfil (catálogos públicos), Recuperar contraseña (flujo real con token) |

**Importante:** un paciente solo puede reservar si existen franjas libres. Al crear un médico nuevo, genere sus horarios en *Admin → Schedules*.

**Sin soporte en el backend (por eso no aparecen):** fotos/valoraciones/años de experiencia de los médicos, documentos adjuntos (las recetas se muestran como "Prescriptions"), notificaciones para médico/recepción (el backend solo notifica al paciente) y cambio de contraseña desde Ajustes (se usa "Reset password").

## Seguridad
JWT sin estado, RBAC por rol, contraseñas con BCrypt, logout con lista negra de tokens en memoria (con varias instancias, sustitúyala por almacén compartido), respuesta uniforme en recuperación de contraseña, auditoría de accesos y cambios clínicos (RNF-14). **RNF-03:** habilite HTTPS (`server.ssl.*` o proxy inverso) y `sslMode=REQUIRED` en `DB_URL` en producción.

## Fuera de alcance del backend (declarado)
Aplicación móvil nativa, envío real de correo/SMS/push (hay un `NotificadorCorreo` que solo registra en log), backups (RNF-15) y alta disponibilidad (RNF-16) son de infraestructura.
