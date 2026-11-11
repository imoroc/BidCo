import { expect, test } from '@playwright/test';
import { RUTAS_PUBLICAS, simularApi } from './utiles';

test.describe('Rutas públicas', () => {
  for (const { ruta, titulo } of RUTAS_PUBLICAS) {
    test(`${ruta} carga con su título y sin errores`, async ({ page }) => {
      const errores: string[] = [];
      page.on('pageerror', error => errores.push(error.message));
      await simularApi(page);
      await page.goto(ruta);
      await expect(page).toHaveTitle(titulo);
      await page.waitForLoadState('networkidle');
      expect(errores).toEqual([]);
    });
  }

  test('la raíz redirige a las subastas', async ({ page }) => {
    await simularApi(page);
    await page.goto('/');
    await expect(page).toHaveURL(/\/auction$/);
  });

  test('una ruta desconocida muestra la página de error', async ({ page }) => {
    await simularApi(page);
    await page.goto('/esta-ruta-no-existe');
    await expect(page).toHaveTitle('Página no encontrada · BidCo');
  });
});
