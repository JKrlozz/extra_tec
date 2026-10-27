-- usuario de mysql con permisos mínimos para la api
-- cambiar la contraseña y copiarla en db_password de api/.env
CREATE USER IF NOT EXISTS 'extra_tec_app'@'localhost' IDENTIFIED BY 'CAMBIA_ESTA_CLAVE';
GRANT SELECT, INSERT, UPDATE, DELETE ON extra_tec.* TO 'extra_tec_app'@'localhost';
FLUSH PRIVILEGES;
