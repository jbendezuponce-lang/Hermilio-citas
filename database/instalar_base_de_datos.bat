@echo off
REM Importa la base de datos hermilio_citas en el MySQL local (Windows).
REM Requiere que "mysql" este en el PATH (o editar la ruta de abajo).
set /p DBUSER=Usuario de MySQL [root]: 
if "%DBUSER%"=="" set DBUSER=root
mysql -u %DBUSER% -p < "%~dp0hermilio_citas_setup.sql"
if %ERRORLEVEL%==0 (echo Base de datos instalada correctamente.) else (echo ERROR: revisa usuario/clave o que MySQL este encendido.)
pause
