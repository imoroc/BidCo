-- Índices para las consultas más habituales (listados, pujas de una subasta, subastas de un usuario).
CREATE INDEX IF NOT EXISTS idx_puja_subasta_importe ON puja (subasta_id, importe DESC);
CREATE INDEX IF NOT EXISTS idx_puja_usuario ON puja (usuario_id);
CREATE INDEX IF NOT EXISTS idx_subasta_fecha_final ON subasta (fecha_final);
CREATE INDEX IF NOT EXISTS idx_subasta_creador ON subasta (creador_id);
CREATE INDEX IF NOT EXISTS idx_subasta_ganador ON subasta (ganador_id);
CREATE INDEX IF NOT EXISTS idx_subasta_tipo_fecha_final ON subasta (subasta_normal, fecha_final);
CREATE INDEX IF NOT EXISTS idx_comentario_subasta ON comentario (subasta_id);
CREATE INDEX IF NOT EXISTS idx_puja_sorteo_sorteo_usuario ON puja_sorteo (sorteo_id, usuario_id);
CREATE INDEX IF NOT EXISTS idx_puja_sorteo_usuario ON puja_sorteo (usuario_id);
CREATE INDEX IF NOT EXISTS idx_sorteo_fecha_fin ON sorteo (fecha_fin);

-- Restricciones de integridad. Se crean NOT VALID: se aplican a las filas nuevas o modificadas sin
-- escanear ni rechazar los datos que ya existen (se pueden validar después con VALIDATE CONSTRAINT).
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_saldo CHECK (saldo >= 0) NOT VALID;
ALTER TABLE puja ADD CONSTRAINT ck_puja_importe CHECK (importe > 0) NOT VALID;
ALTER TABLE puja_sorteo ADD CONSTRAINT ck_puja_sorteo_puntos CHECK (puntos > 0) NOT VALID;
ALTER TABLE subasta ADD CONSTRAINT ck_subasta_precios CHECK (
    (precio_inicial IS NULL OR precio_inicial >= 0) AND (precio_final IS NULL OR precio_final >= 0)) NOT VALID;
ALTER TABLE comentario ADD CONSTRAINT ck_comentario_precio CHECK (precio_estimado >= 0) NOT VALID;
