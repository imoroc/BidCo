# BidCo · Frontend

Cliente web de BidCo, una plataforma de subastas simuladas, subastas a ciegas y sorteos.

## Stack

- Angular 22.2 con componentes standalone, carga diferida de rutas y títulos por ruta.
- TypeScript 6.0.
- Chart.js 4 para las estadísticas del perfil.
- `jwt-decode` para leer el rol y la caducidad del token.
- Montserrat autoalojada (`@fontsource-variable/montserrat`).
- Pruebas con Jasmine y Karma.

## Puesta en marcha

Requisitos: Node.js 22 y npm.

```bash
npm ci
npm start
```

La aplicación queda en `http://localhost:4200`. Las llamadas a `/api` usan la URL de `src/environments/`.

## Entornos

La URL de la API está en `src/environments/`:

| Fichero | Uso | `apiUrl` |
|---|---|---|
| `environment.development.ts` | `npm start` | `http://localhost:8080/api` |
| `environment.ts` | build de producción | URL del backend desplegado |

## Scripts

| Comando | Qué hace |
|---|---|
| `npm start` | Servidor de desarrollo. |
| `npm test` | Pruebas unitarias con Karma (en el CI: `npm test -- --watch=false --browsers=ChromeHeadlessCI`). |
| `npm run build -- --configuration production` | Build de producción en `dist/frontend`. |
| `npm run test:e2e` | Pruebas de extremo a extremo con Playwright sobre el build de producción (primero hay que compilar; la primera vez, `npx playwright install chromium`). |

## Estructura

```
src/app/
  components/    Piezas reutilizables (cabecera, pie, filtro, tarjeta de producto, toasts...)
  views/         Una carpeta por página
  services/      Acceso a la API, autenticación y guards
  interceptors/  Token JWT y gestión de errores HTTP
  utils/         Fechas, filtrado de listados y loader de imágenes de Cloudinary
```

## Pruebas e2e

Las pruebas de `e2e/` usan Playwright y sirven `dist/frontend/browser` con un servidor estático propio. La API se simula con `page.route`, así que no hacen falta backend ni base de datos. Se ejecutan en tres proyectos (escritorio, móvil y modo oscuro):

- `humo.spec.ts`: cada ruta pública carga con su título y sin errores de JavaScript.
- `listado.spec.ts`: el listado de subastas muestra lo que devuelve la API y el aviso de «sin resultados».
- `accesibilidad.spec.ts`: axe-core (WCAG 2.1 A y AA) no encuentra infracciones graves ni críticas.
- `capturas.spec.ts`: guarda una captura por vista y proyecto en `e2e-resultados/capturas`; el CI las sube como artefacto `capturas`.

## Convenciones

- Los estilos globales y las variables de diseño están en `src/styles.css`; los componentes usan esas variables en lugar de colores sueltos.
- Los importes se muestran con el locale `es-ES` (pipe `currency`).
- Las imágenes de los listados se cargan con `NgOptimizedImage` y el loader de Cloudinary.

## Despliegue

El frontend se despliega en Vercel. `vercel.json` reescribe todas las rutas a `index.html` para que funcione el enrutado de Angular.
