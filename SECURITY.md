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
- CodeQL para análisis estático de Java y TypeScript.
- Secretos de ejecución suministrados mediante variables de entorno.
- CORS con orígenes permitidos explícitos.
- JWT firmado con una clave externa al repositorio y con longitud mínima.
- Verificación de propiedad para operaciones sobre perfiles.
- DTO público separado para no exponer correo, dirección ni saldo del usuario.

Las garantías anteriores describen los controles implementados en el código y el pipeline; no sustituyen una auditoría de seguridad independiente ni una revisión de la configuración del proveedor de despliegue.
