-- Revocación de sesiones: cada JWT de usuario lleva la versión con la que se emitió y el filtro la compara
-- con esta columna. Subirla (cambio o restablecimiento de contraseña, borrado de cuenta) invalida de golpe
-- todos los tokens anteriores de esa cuenta. Los tokens ya emitidos no llevan versión y cuentan como 0,
-- que es el valor de todas las cuentas existentes: nadie pierde la sesión al desplegar.
ALTER TABLE usuario ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
