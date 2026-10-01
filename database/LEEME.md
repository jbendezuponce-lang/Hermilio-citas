# Cómo usar el proyecto en otra PC (pasos para tus compañeros)

## Requisitos (instalar una vez)
- Java 21, Maven y MySQL 8.x (con su usuario y contraseña).

## Pasos
1. Descomprime el zip.
2. **Instala la base de datos** (elige una forma):
   - Windows: doble clic en `database/instalar_base_de_datos.bat`
   - Workbench: File → Open SQL Script → `database/hermilio_citas_setup.sql` → botón del rayo.
   - Terminal: `mysql -u root -p < database/hermilio_citas_setup.sql`
3. Si tu MySQL **no** usa `root` / `root`, antes de arrancar define:
   - Windows (cmd):  `set DB_USERNAME=root` y `set DB_PASSWORD=tu_clave`
   - Linux/Mac:      `export DB_USERNAME=root DB_PASSWORD=tu_clave`
4. En la carpeta del proyecto: `mvn clean spring-boot:run`
5. Abre `medical-system/index.html` (mejor con Live Server en VS Code).
6. Entra con `admin@hermilio.com` / `admin123`.

## ¿Quieres llevar también los datos que ya registraste?
En la PC original exporta: `mysqldump -u root -p --databases hermilio_citas > database/hermilio_citas_datos.sql`
y mete ese archivo en el zip. En la otra PC impórtalo en lugar del setup:
`mysql -u root -p < database/hermilio_citas_datos.sql`
