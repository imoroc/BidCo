-- =====================================================================================================
-- Reabrir el catálogo: vuelve a poner en circulación las subastas y sorteos que ya han vencido.
--
-- ES UN SCRIPT MANUAL, NO UNA MIGRACIÓN DE FLYWAY: se ejecuta a mano (psql, DBeaver, consola de Render…)
-- cuando se quiere repoblar la web. No lo ejecuta la aplicación ni el CI. Es seguro repetirlo.
--
-- ANTES DE EJECUTARLO: haz una copia de la base de datos.
--
-- Qué hace
--   1. Subastas y sorteos vencidos SIN ganador: se les alarga la fecha de cierre. No se toca nada más:
--      las pujas y participaciones que tuvieran se conservan.
--   2. Subastas y sorteos vencidos CON ganador: se mantienen cerrados (son historial) y se publica una
--      copia nueva, sin pujas ni ganador, con el mismo artículo, descripción, imagen y categoría.
--
-- Por qué no se reabren los que ya tienen ganador
--   Al cerrar una subasta se reparten puntos al ganador (100 en las normales, 200 en las ciegas) y se le
--   envía un aviso. Si se reabriera, al volver a cerrarla se repartirían otra vez y habría puntos de más.
--   La copia nueva evita ese problema y deja el historial intacto.
--
-- Fechas
--   El cierre queda a la duración indicada abajo (10 años por defecto) más un desfase de 0 a 364 días
--   según el identificador, para que no cierren todas el mismo día y funcione el orden por fecha.
--
-- Cómo usarlo
--   Por defecto el script termina con ROLLBACK: muestra el resumen y no guarda nada, para poder revisarlo.
--   Cuando el resumen sea el esperado, cambia la última línea de ROLLBACK a COMMIT y vuelve a ejecutarlo.
-- =====================================================================================================

BEGIN;

CREATE TEMP TABLE parametros ON COMMIT DROP AS
SELECT interval '10 years' AS duracion,                         -- <-- ajusta aquí la duración
       (now() AT TIME ZONE 'Europe/Madrid')::date AS hoy;

CREATE TEMP TABLE antes ON COMMIT DROP AS
SELECT (SELECT count(*) FROM subasta WHERE fecha_final > now()) AS subastas_abiertas,
       (SELECT count(*) FROM sorteo  WHERE fecha_fin   > now()) AS sorteos_abiertos;

-- 1a. Subastas vencidas sin ganador: se alarga el cierre.
UPDATE subasta s
   SET fecha_final = now() + p.duracion + (s.subasta_id % 365) * interval '1 day'
  FROM parametros p
 WHERE s.fecha_final <= now()
   AND s.ganador_id IS NULL;

-- 1b. Sorteos vencidos sin ganador: se alarga el cierre.
UPDATE sorteo s
   SET fecha_fin = now() + p.duracion + (s.sorteo_id % 365) * interval '1 day'
  FROM parametros p
 WHERE s.fecha_fin <= now()
   AND s.ganador_id IS NULL;

-- 2a. Subastas cerradas con ganador: una copia nueva por artículo (la más reciente), sin duplicar si ya
--     hay una abierta con el mismo creador, nombre e imagen.
INSERT INTO subasta (fecha_inicial, fecha_final, precio_inicial, precio_final, subasta_normal,
                     nombre_articulo, descripcion, imagen, creador_id, ganador_id, categoria)
SELECT p.hoy,
       now() + p.duracion + (o.subasta_id % 365) * interval '1 day',
       COALESCE(o.precio_inicial, 0), COALESCE(o.precio_inicial, 0), o.subasta_normal,
       o.nombre_articulo, o.descripcion, o.imagen, o.creador_id, NULL, o.categoria
  FROM (SELECT DISTINCT ON (creador_id, nombre_articulo, imagen) *
          FROM subasta
         WHERE ganador_id IS NOT NULL AND fecha_final <= now()
         ORDER BY creador_id, nombre_articulo, imagen, fecha_final DESC) o
 CROSS JOIN parametros p
 WHERE NOT EXISTS (SELECT 1
                     FROM subasta a
                    WHERE a.creador_id = o.creador_id
                      AND a.nombre_articulo = o.nombre_articulo
                      AND a.imagen IS NOT DISTINCT FROM o.imagen
                      AND a.fecha_final > now());

-- 2b. Sorteos cerrados con ganador: misma idea.
INSERT INTO sorteo (nombre_articulo, descripcion, fecha_inicio, fecha_fin, puntos_necesarios, puntos_finales,
                    trabajador_id, ganador_id, imagen, categoria)
SELECT o.nombre_articulo, o.descripcion, p.hoy,
       now() + p.duracion + (o.sorteo_id % 365) * interval '1 day',
       o.puntos_necesarios, 0, o.trabajador_id, NULL, o.imagen, o.categoria
  FROM (SELECT DISTINCT ON (trabajador_id, nombre_articulo, imagen) *
          FROM sorteo
         WHERE ganador_id IS NOT NULL AND fecha_fin <= now()
         ORDER BY trabajador_id, nombre_articulo, imagen, fecha_fin DESC) o
 CROSS JOIN parametros p
 WHERE NOT EXISTS (SELECT 1
                     FROM sorteo a
                    WHERE a.trabajador_id = o.trabajador_id
                      AND a.nombre_articulo = o.nombre_articulo
                      AND a.imagen = o.imagen
                      AND a.fecha_fin > now());

-- Resumen: abiertas antes y después.
SELECT 'subastas abiertas' AS concepto,
       (SELECT subastas_abiertas FROM antes) AS antes,
       (SELECT count(*) FROM subasta WHERE fecha_final > now()) AS despues
UNION ALL
SELECT 'sorteos abiertos',
       (SELECT sorteos_abiertos FROM antes),
       (SELECT count(*) FROM sorteo WHERE fecha_fin > now());

ROLLBACK;  -- <-- cambia a COMMIT para guardar los cambios
