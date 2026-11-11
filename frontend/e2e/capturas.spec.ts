import { test } from '@playwright/test';
import { RUTAS_PUBLICAS, simularApi } from './utiles';

// Genera capturas de las vistas públicas en escritorio, móvil y modo oscuro. El CI las sube como
// artefacto «capturas»: así se revisa el aspecto sin abrir el navegador a mano.
test.describe('Capturas', () => {
  for (const { ruta } of RUTAS_PUBLICAS) {
    test(`captura de ${ruta}`, async ({ page }, info) => {
      await simularApi(page);
      await page.goto(ruta);
      await page.waitForLoadState('networkidle');
      await page.evaluate(() => document.fonts.ready);
      const nombre = ruta.slice(1).replaceAll('/', '-');
      await page.screenshot({ path: `e2e-resultados/capturas/${info.project.name}/${nombre}.png`, fullPage: true });
    });
  }
});
