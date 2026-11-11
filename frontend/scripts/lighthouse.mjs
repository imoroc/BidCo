// Lighthouse sobre el build de producción (escritorio). Sirve dist/frontend/browser con el
// servidor estático de e2e, usa el Chromium de Playwright y comprueba los umbrales de abajo.
// La API del backend se bloquea: así las medidas no dependen de la red ni de que Render esté despierto.
import { mkdirSync, writeFileSync } from 'node:fs';
import lighthouse from 'lighthouse';
import escritorio from 'lighthouse/core/config/desktop-config.js';
import { chromium } from '@playwright/test';

const PUERTO_WEB = 4301;
const PUERTO_DEPURACION = 9222;
const SALIDA = 'e2e-resultados/lighthouse';
const RUTAS = ['/auction', '/login', '/ayuda'];
const UMBRALES = { performance: 90, accessibility: 95, seo: 90 };
const NOMBRES = { performance: 'rendimiento', accessibility: 'accesibilidad', seo: 'SEO', 'best-practices': 'buenas prácticas' };

process.argv[2] = String(PUERTO_WEB);
await import('../e2e/servidor-estatico.mjs');

const navegador = await chromium.launch({ args: [`--remote-debugging-port=${PUERTO_DEPURACION}`] });
mkdirSync(SALIDA, { recursive: true });
let fallos = 0;

try {
  for (const ruta of RUTAS) {
    const { lhr } = await lighthouse(
      `http://localhost:${PUERTO_WEB}${ruta}`,
      { port: PUERTO_DEPURACION, output: 'json', logLevel: 'error', blockedUrlPatterns: ['*onrender.com*'] },
      escritorio
    );
    writeFileSync(`${SALIDA}/${ruta.slice(1)}.json`, JSON.stringify(lhr));

    const puntos = Object.entries(lhr.categories).map(([id, c]) => [id, Math.round((c.score ?? 0) * 100)]);
    console.log(`::notice title=Lighthouse ${ruta}::${puntos.map(([id, p]) => `${NOMBRES[id] ?? id} ${p}`).join(', ')}`);

    for (const [id, minimo] of Object.entries(UMBRALES)) {
      const obtenido = puntos.find(([categoria]) => categoria === id)?.[1] ?? 0;
      if (obtenido >= minimo) continue;
      fallos++;
      const pendientes = lhr.categories[id].auditRefs
        .map(ref => lhr.audits[ref.id])
        .filter(a => a.score !== null && a.score < 0.9 && a.scoreDisplayMode !== 'informative')
        .slice(0, 8)
        .map(a => `${a.id} (${Math.round(a.score * 100)})`);
      console.log(`::warning title=Lighthouse ${ruta}::${NOMBRES[id]} ${obtenido} < ${minimo}. Auditorías: ${pendientes.join(', ')}`);
    }
  }
} finally {
  await navegador.close();
}

// El servidor estático sigue escuchando: se sale explícitamente.
process.exit(fallos > 0 ? 1 : 0);
