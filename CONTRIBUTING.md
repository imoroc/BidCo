# Contribuir a BidCo

Gracias por querer colaborar. Esta guía resume cómo trabajar en el repositorio.

## Antes de empezar

1. Haz un fork o clona el repositorio y parte siempre de la rama `deploy`.
2. Sigue la sección «Puesta en marcha» del [README](README.md) para tener backend, frontend y base de datos en local.
3. Si vas a proponer un cambio grande, abre antes una issue para comentarlo.

## Flujo de trabajo

- Crea una rama por tema a partir de `deploy`: `mejora/<tema>`, `correccion/<tema>` o similar.
- Haz commits pequeños, uno por cambio lógico, y súbelos con frecuencia.
- Abre la PR hacia `deploy` en cuanto tengas el primer commit: el CI solo se ejecuta con una PR abierta.
- Rellena la plantilla de la PR y espera a que pasen todos los checks antes de fusionar.
- Pon a la PR una etiqueta (`enhancement`, `bug`, `documentation` o `dependencies`): las notas de cada release se generan agrupando las PR por etiqueta (`.github/release.yml`).

## Mensajes de commit

En español, en infinitivo y sin punto final, describiendo qué hace el cambio:

```
Añadir el filtro por rango de precio en los sorteos
Corregir el cierre de subastas sin pujas
Eliminar el CSS heredado sin uso
```

## Comprobaciones locales

```bash
# Frontend
cd frontend
npm ci
npm test -- --watch=false
npm run build -- --configuration production

# Backend
cd backend
./mvnw clean verify
```

## Qué debe cumplir una PR

- Compila y pasa las pruebas del frontend y del backend.
- Incluye pruebas para la lógica nueva o corregida.
- No introduce secretos, tokens ni contraseñas en ningún commit, tampoco en las pruebas: genera los valores de prueba en lugar de escribirlos literales.
- No duplica código: SonarCloud exige un máximo del 3 % de duplicación en el código nuevo.
- Mantiene los estilos con las variables de diseño de `frontend/src/styles.css` y el CSS de cada componente por debajo de 4 kB.
- Si cambia lo que ve el usuario, adjunta capturas en escritorio y móvil.
- Actualiza la documentación si cambia el comportamiento o las variables de entorno.

## Checks que se ejecutan en cada PR

CodeQL, SonarCloud, GitGuardian, revisión de dependencias, el CI del frontend y del backend, y el despliegue de previsualización de Vercel. Todos deben estar en verde.

## Detalles útiles

- `backend/mvnw.cmd` puede aparecer como modificado solo por los saltos de línea; no lo incluyas en el commit. Añade siempre rutas concretas con `git add`, nunca `git add -A`.
- Las categorías se guardan en minúsculas y sin tilde (`tecnologia`, `moda`...).
- Las decisiones técnicas y sus límites están en [`docs/decisiones-tecnicas.md`](docs/decisiones-tecnicas.md).

## Seguridad

Si encuentras una vulnerabilidad, no abras una issue pública: sigue las indicaciones de [`SECURITY.md`](SECURITY.md).
