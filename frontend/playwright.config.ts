import { defineConfig, devices } from '@playwright/test';

const PUERTO = 4300;
const enCI = !!process.env['CI'];

// Las pruebas e2e sirven la compilación de producción (`npm run build -- --configuration production`)
// y simulan la API, así que no necesitan backend ni base de datos.
export default defineConfig({
  testDir: 'e2e',
  outputDir: 'e2e-resultados/ejecuciones',
  fullyParallel: true,
  forbidOnly: enCI,
  retries: enCI ? 1 : 0,
  reporter: enCI ? [['github'], ['list']] : 'list',
  use: {
    baseURL: `http://localhost:${PUERTO}`,
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    trace: 'retain-on-failure'
  },
  projects: [
    { name: 'escritorio', use: { ...devices['Desktop Chrome'] } },
    { name: 'movil', use: { ...devices['Pixel 7'] } },
    { name: 'oscuro', use: { ...devices['Desktop Chrome'], colorScheme: 'dark' } }
  ],
  webServer: {
    command: `node e2e/servidor-estatico.mjs ${PUERTO}`,
    url: `http://localhost:${PUERTO}`,
    reuseExistingServer: !enCI
  }
});
