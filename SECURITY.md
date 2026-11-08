# Security Policy

## Versiones soportadas

| Versión | Soporte |
|---|---|
| `deploy` | Activa |
| `master` | Sin mantenimiento |

## Reportar una vulnerabilidad

No publiques vulnerabilidades explotables en issues públicos. Utiliza la función **Report a vulnerability** de la pestaña Security del repositorio.

Incluye:
- Descripción e impacto potencial.
- Pasos mínimos para reproducir el problema.
- Commit o versión afectados.
- Cualquier mitigación conocida.

## Controles del proyecto

- Dependabot para actualizaciones de dependencias.
- Análisis de código mediante la configuración predeterminada de CodeQL de GitHub.
- Secretos de ejecución suministrados mediante variables de entorno.
- CORS con orígenes permitidos configurables por variable de entorno y cabeceras de seguridad HTTP.
- JWT firmado con una clave externa al repositorio y con longitud mínima, con expiración e emisor verificados.
- Límite de intentos de inicio de sesión (respuesta 429) y mensaje de error único para credenciales incorrectas.
- Sesión de administración con cambio de identificador al autenticarse y protección CSRF mediante cabecera.
- Verificación de propiedad para operaciones sobre perfiles, cuentas de trabajadores y comentarios.
- Alta de trabajadores restringida a sesiones de administración.
- Pujas validadas en el servidor (fecha, precio, saldo y creador) dentro de transacciones con bloqueo de fila.
- Cierre de subastas y sorteos idempotente, solo cuando el plazo ha terminado, y sorteos con `SecureRandom`.
- Subastas a ciegas: el importe máximo no se revela mientras están abiertas.
- Validación de imágenes subidas (tipo, tamaño y firma del archivo).
- DTO público separado para no exponer correo, dirección ni saldo del usuario.
- Swagger/OpenAPI desactivado por defecto y contenedor Docker sin usuario root.
- Contraseñas almacenadas con BCrypt, incluidas las cuentas de ejemplo de `database/schema_postgres.sql`.

## Limitaciones conocidas

- El token JWT se guarda en `localStorage` en el navegador, por lo que un fallo XSS permitiría robarlo. Una cookie `HttpOnly` reduciría ese riesgo.
- `database/schema_postgres.sql` es solo para desarrollo y demos: elimina las tablas existentes y crea usuarios de ejemplo con contraseñas de demostración.

Las garantías anteriores describen los controles implementados en el código y el pipeline; no sustituyen una auditoría de seguridad independiente ni una revisión de la configuración del proveedor de despliegue.
