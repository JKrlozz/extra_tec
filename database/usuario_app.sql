-- Usuario de MySQL con permisos minimos para la API
-- Cambiar la contrasena antes de usar la API
CREATE USER IF NOT EXISTS 'extra_tec_app'@'localhost' IDENTIFIED BY 'CAMBIA_ESTA_CLAVE';
GRANT SELECT, INSERT, UPDATE, DELETE ON extra_tec.* TO 'extra_tec_app'@'localhost';
FLUSH PRIVILEGES;
