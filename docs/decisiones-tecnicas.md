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

## Límite general de peticiones (`RateLimitFilter`)

- Filtro que cuenta las peticiones a `/api/*` por IP en ventanas fijas. Se ejecuta después de CORS
  y antes del filtro JWT, así que el `429` (con cabecera `Retry-After`) llega legible al navegador.
- Tope general de 300 peticiones por minuto y topes propios más estrictos: registro (5 por hora),
  pujas de subasta y de sorteo (30 por minuto), recarga de saldo (10 por hora) y creación de
  subastas, que sube imágenes (10 por hora). `GET /api/ping` y el preflight `OPTIONS` no cuentan.
- Se ajusta con `RATE_LIMIT_GENERAL_PER_MINUTE` y se desactiva con `RATE_LIMIT_ENABLED=false`.
- Mismas limitaciones que `LoginRateLimiter`: estado en memoria, se reinicia al redesplegar y no se
  comparte entre instancias. Por IP solamente: varios usuarios tras una misma IP comparten cupo.
- Se usó un contador propio para no añadir una dependencia; si se escala a varias instancias, el
  siguiente paso es un almacén compartido o el limitador del proxy.

## CSP del frontend (`vercel.json`)

- Se publica como `Content-Security-Policy-Report-Only`: el navegador avisa en la consola de las
  infracciones, pero no bloquea nada. Orígenes permitidos: el propio sitio, la API en Render y las
  imágenes de Cloudinary (más `data:` y `blob:` para las vistas previas de imagen).
- `style-src` mantiene `'unsafe-inline'` porque Angular inserta los estilos de los componentes en
  elementos `<style>`. Quitarlo exige un nonce (`ngCspNonce`) que Vercel no genera con un sitio estático.
- El build de producción inserta el CSS crítico con un manejador `onload` en línea, que `script-src 'self'`
  bloquearía. Antes de pasar a modo estricto hay que desactivar `inlineCritical` o aceptar un hash.
- Pasos pendientes: navegar por todas las vistas con la consola abierta, corregir lo que se vea
  y, cuando no queden avisos, cambiar la cabecera a `Content-Security-Policy`.
- `Cross-Origin-Opener-Policy: same-origin` ya se aplica. HSTS lo añade Vercel en su dominio.

## Revocación de sesiones de usuario

- Cada cuenta tiene una `token_version` (columna `usuario.token_version`, migración V5). El JWT de usuario
  lleva esa versión en el claim `tv`, y `JwtFilter` la compara en cada petición autenticada con la de la base
  de datos (una consulta por clave primaria). Si no coinciden, o la cuenta ya no existe, responde 401 y el
  frontend cierra la sesión.
- La versión sube al cambiar la contraseña, al restablecerla por correo y al eliminar la cuenta. Al cambiarla
  desde el perfil, la API devuelve un token nuevo para que el dispositivo actual no pierda la sesión; el resto
  quedan cerradas.
- Los tokens emitidos antes de la V5 no llevan `tv` y cuentan como versión 0, igual que todas las cuentas
  existentes: nadie pierde la sesión al desplegar.
- No se acortó la vida del JWT (24 h) porque, sin *refresh token*, obligaría a volver a entrar a mitad de
  una subasta. Con la revocación cubierta, acortarla solo tiene sentido junto a una renovación silenciosa.
- Los tokens de trabajadores y expertos no se revocan por este mecanismo.

## Sesión de administración persistente (Spring Session JDBC)

- La sesión del panel de administración (`HttpSession`, atributo `adminId`) se guarda en PostgreSQL con
  Spring Session JDBC en lugar de en la memoria de Tomcat: sobrevive a reinicios y redespliegues y es válida si
  hay varias instancias de la API. Se mantiene el modelo actual (cookie de sesión, cabecera CSRF en el filtro y
  expiración por inactividad de 30 minutos); solo cambia dónde se almacena.
- Las tablas `spring_session` y `spring_session_attributes` las crea Flyway (V6, esquema oficial de
  `spring-session-jdbc`), con `spring.session.jdbc.initialize-schema=never`. Spring borra las caducadas cada minuto.
- El nombre de la cookie pasa de `JSESSIONID` a `SESSION`: las sesiones abiertas en el momento del despliegue se
  pierden una vez y el administrador tiene que volver a entrar.
- Solo el panel de administración usa sesiones: el resto de la API es sin estado (JWT), así que la tabla solo
  recibe filas cuando un administrador inicia sesión.
