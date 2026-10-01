#!/usr/bin/env bash
# Importa la base de datos hermilio_citas en el MySQL local (Linux/Mac).
read -p "Usuario de MySQL [root]: " U; U=${U:-root}
mysql -u "$U" -p < "$(dirname "$0")/hermilio_citas_setup.sql" \
  && echo "Base de datos instalada correctamente." \
  || echo "ERROR: revisa usuario/clave o que MySQL este encendido."
