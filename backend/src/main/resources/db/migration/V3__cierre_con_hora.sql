-- El cierre de subastas y sorteos pasa de un día natural a un instante exacto (timestamptz).
-- Hasta ahora la fecha final era de día completo: se podía participar durante todo ese día y se cerraba
-- al empezar el siguiente. Las filas existentes se convierten a la medianoche siguiente en Europe/Madrid,
-- que es el momento en que se cerraban, así que nada abierto hoy se cierra antes ni después de lo previsto.
-- ::date admite que la columna original sea DATE o TIMESTAMP, según cómo se creara la base.
ALTER TABLE subasta
    ALTER COLUMN fecha_final TYPE timestamptz
    USING (((fecha_final::date) + 1)::timestamp AT TIME ZONE 'Europe/Madrid');

ALTER TABLE sorteo
    ALTER COLUMN fecha_fin TYPE timestamptz
    USING (((fecha_fin::date) + 1)::timestamp AT TIME ZONE 'Europe/Madrid');
