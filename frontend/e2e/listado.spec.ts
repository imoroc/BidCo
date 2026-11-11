import { expect, test } from '@playwright/test';
import { responderApi, simularApi, SUBASTAS } from './utiles';

test.describe('Listado de subastas', () => {
  test('muestra las subastas que devuelve la API', async ({ page }) => {
    await simularApi(page);
    await page.goto('/auction');
    for (const subasta of SUBASTAS) {
      await expect(page.getByText(subasta.nombreArticulo)).toBeVisible();
    }
  });

  test('avisa cuando no hay resultados', async ({ page }) => {
    await simularApi(page);
    await responderApi(page, url => url.pathname.startsWith('/api/subastas/filtro-'), []);
    await page.goto('/auction');
    await expect(page.getByText('No hay subastas que coincidan con los filtros.')).toBeVisible();
  });
});
