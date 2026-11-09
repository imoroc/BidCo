# Decisiones técnicas y limitaciones conocidas

## Fechas y zona horaria

- Las fechas de inicio y fin de subastas y sorteos se guardan en el modelo como `LocalDate`
  (solo día, sin hora ni zona). En `database/schema_postgres.sql` las columnas son `TIMESTAMP`
  (`fecha_inicial`, `fecha_final`, `fecha_inicio`, `fecha_fin`); Hibernate convierte el `LocalDate`
  a medianoche al escribir y descarta la hora al leer.
- Todas las comprobaciones de "hoy" usan el reloj único de `AppClockConfig`
  (`LocalDate.now(clock)`), cuya zona se configura con `app.timezone` (variable `APP_TIMEZONE`,
  por defecto `Europe/Madrid`). Así el día no depende de la zona del servidor donde se despliegue.
- Una subasta o un sorteo se considera vencido cuando el día actual es **posterior** a la fecha
  final: el propio día de la fecha final sigue abierto ("vence hoy") y se cierra al día siguiente.
- Las marcas de tiempo de pujas y participaciones (`fecha` con `CURRENT_TIMESTAMP`) son
  informativas y no intervienen en el cierre.
- Si en el futuro se necesita cierre a una hora concreta, habrá que pasar el modelo a `Instant`
  o `ZonedDateTime` y la columna a `TIMESTAMP WITH TIME ZONE`.

## Límite de intentos (`LoginRateLimiter`)

- Cuenta los intentos fallidos por usuario e IP en una ventana de 15 minutos (máximo 5) y
  protege el inicio de sesión, el de administración y el cambio de contraseña.
- El estado vive **en memoria** del proceso: se reinicia en cada despliegue o reinicio y no se
  comparte entre instancias. Con varias instancias del backend, cada una llevaría su propia cuenta
  y el límite efectivo sería mayor.
- Si se escala a varias instancias habría que moverlo a un almacén compartido (Redis o una tabla
  en PostgreSQL) o a un limitador en el proxy.
