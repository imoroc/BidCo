import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { RUTAS_PUBLICAS, simularApi } from './utiles';

// Se ejecuta en escritorio, móvil y modo oscuro (un proyecto de Playwright cada uno).
test.describe('Accesibilidad (axe, WCAG 2.1 A y AA)', () => {
  for (const { ruta } of RUTAS_PUBLICAS) {
    test(`${ruta} no tiene infracciones graves`, async ({ page }) => {
      await simularApi(page);
      await page.goto(ruta);
      await page.waitForLoadState('networkidle');
      const { violations } = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
        .analyze();
      const graves = violations
        .filter(v => v.impact === 'serious' || v.impact === 'critical')
        .map(v => `${v.id} (${v.impact}, ${v.nodes.length} nodos): ${v.nodes[0]?.target.join(' ')}`);
      expect(graves).toEqual([]);
    });
  }
});
