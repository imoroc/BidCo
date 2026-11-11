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

## Operación: salud e identificador de petición

- `IdPeticionFilter` da a cada petición un identificador que se guarda en el MDC (`idPeticion`), sale
  en cada línea de registro y se devuelve en la cabecera `X-Request-Id`. Si el cliente o el proxy ya
  envían uno de 8 a 64 caracteres (letras, números, `.`, `_`, `-`) se conserva; cualquier otro valor
  se descarta y se genera un UUID, para que no se puedan inyectar saltos de línea en los registros.
- Actuator solo expone `health`, sin detalles (`show-details=never`): sirve a un monitor externo o a
  la comprobación de salud de la plataforma sin filtrar información interna. `GET /actuator/health`
  es la única ruta pública de `/actuator`; el resto cae en el «denegar por defecto» del `JwtFilter`.
- `GET /api/ping` se mantiene: lo usa `keep-alive.yml` y además hace una consulta real a la base.
- Pendiente: logs en JSON, seguimiento de errores (Sentry) y sustituir `keep-alive.yml` por un
  monitor externo o un plan sin suspensión.

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

## Cobertura de pruebas del backend (JaCoCo)

- El CI publica en el resumen de la ejecución la cobertura medida (líneas, ramas e instrucciones, y líneas por
  paquete) y sube el informe HTML como artefacto (`cobertura-backend`, 14 días).
- `mvn verify` falla si la cobertura de líneas del conjunto baja de `jacoco.minimo.lineas` (propiedad del
  `pom.xml`, hoy 0,48). Se fijó unos 2,6 puntos por debajo de lo medido el 11-nov-2026 (50,6 %) para no romper
  el CI por pequeñas variaciones. La regla es no bajarlo: cuando la cobertura suba de forma sostenida, se sube el mínimo.
- El número incluye código generado (las implementaciones de MapStruct, hoy al 0 %) y las clases de DTO, así que
  infravalora la cobertura de la lógica. Excluirlos cambiaría la medición y exige fijar de nuevo el mínimo con el
  dato real, por eso no se ha hecho aquí.
- Es un suelo contra regresiones, no una medida de calidad: no dice si las pruebas comprueban lo importante.

## Avisos por correo de subastas

- Se avisa de dos hechos: «te han superado» (quien lideraba, cuando otra puja pasa a liderar una subasta normal)
  y «has ganado» (al cerrarse una subasta con pujas). Es lo que hace una casa de subastas grande: aviso de servicio
  ligado a una acción propia, no publicidad.
- En las subastas a ciegas no hay aviso de «te han superado»: confirmaría que otra puja es mayor. El de ganador sí,
  porque al cierre el resultado ya es público.
- Solo reciben avisos las cuentas con el correo verificado, nunca las anonimizadas (`@anonimo.invalid`) y solo si
  Brevo está configurado. No se avisa a quien se supera a sí mismo.
- El correo se envía tras confirmar la transacción (`afterCommit`), de modo que una puja rechazada o revertida no
  avisa a nadie. El envío es asíncrono y un fallo del proveedor se registra sin afectar a la puja ni al cierre.
- El nombre del artículo lo escribe un usuario: se escapa antes de ir dentro del HTML del correo.
- No hay ajuste para desactivarlos. Añadirlo exige una columna en `usuario` (migración nueva); queda para otra PR.

## Editar y cancelar la subasta propia

- El creador puede cambiar nombre, descripción y categoría, o retirar la subasta, solo mientras no tenga pujas ni
  haya cerrado. Una vez que alguien ha pujado, lo que se ofrece ya no puede cambiar. No se pueden tocar precio,
  fechas ni tipo.
- La comprobación y el cambio van bajo el mismo bloqueo de fila que las pujas: una puja simultánea no se cuela.
- Cancelar borra la subasta (no hay pujas) y los comentarios de expertos asociados, y devuelve los 50 puntos que
  dio crearla. La imagen se queda en Cloudinary: el servicio no tiene operación de borrado.
- En «Tus subastas» cada tarjeta abierta muestra «Editar» y «Cancelar subasta» (con confirmación). El listado no
  informa de si hay pujas, así que el botón se ofrece siempre y el servidor decide; si responde 409 se explica el
  motivo. Evita una consulta extra por tarjeta; se puede afinar añadiendo un indicador a la respuesta del listado.

## Moderación de subastas

- La administración puede retirar una subasta (`POST /api/admin/subastas/{id}/retirar`, con un motivo obligatorio de
  hasta 500 caracteres). Las cuentas de experto comparten la sesión de `/api/admin`, pero reciben 403.
- Se borra, no se oculta: ocultar exige una migración y tocar todas las consultas de listados. El registro de quién
  la retiró y por qué queda en el log del servidor (el motivo va sin saltos de línea, para evitar inyección en el
  registro). Si se quiere conservar el histórico, es una PR aparte con migración.
- A diferencia del creador, la administración puede retirar una subasta con pujas: se devuelve el saldo retenido a
  quien lideraba (el resto ya lo recuperó al ser superado), se borran pujas y comentarios y se retiran al creador los
  50 puntos de crearla. Si ya tiene ganador no se puede (409): los puntos ya se repartieron y el resultado es definitivo.
- Se avisa por correo al creador (con el motivo, escapado) y a quien lideraba (con el importe devuelto), con las
  mismas reglas que los demás avisos: solo cuentas verificadas y tras confirmar la transacción.
- En la administración, «Moderar subastas» busca por nombre (primeros 12 resultados) y pide el motivo antes de confirmar.

## Historial de pujas
- `GET /api/pujas/historial/{subastaId}` es público y devuelve las 20 pujas más altas (importe y fecha), no el identificador del pujador: así el historial no permite enlazar a una persona con sus pujas en otras subastas.
- El pujador se muestra con la inicial y asteriscos («M***»). Quien activó «pujar de forma anónima» aparece como «Anónimo».
- En una subasta a ciegas el historial no se publica hasta que termina el plazo, igual que `/pujas/mayor`; después se muestra con el mismo enmascarado.
- La ficha de las subastas normales lo recarga cada vez que el flujo en directo informa de un precio nuevo.
- La fecha de la puja se guarda como día (sin hora), de modo que el orden entre pujas del mismo día lo da el importe.
