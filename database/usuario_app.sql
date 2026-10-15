-- Usuario de MySQL con permisos mínimos para la API (no uses root en el .env).
-- Cambia la contraseña antes de ejecutar y ponla igual en api/.env (DB_PASSWORD).
CREATE USER IF NOT EXISTS 'extra_tec_app'@'localhost' IDENTIFIED BY 'CAMBIA_ESTA_CLAVE';
GRANT SELECT, INSERT, UPDATE, DELETE ON extra_tec.* TO 'extra_tec_app'@'localhost';
FLUSH PRIVILEGES;
