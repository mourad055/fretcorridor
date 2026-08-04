import { defineConfig, devices } from '@playwright/test';

/**
 * PRD §8.3 : un scénario E2E critique par rôle et par sprint livrant un écran ;
 * un scénario dédié à la garde de routes (URL directe hors rôle → 403 propre,
 * sans fuite de données dans la réponse réseau).
 *
 * Port web non standard (4201) : cette machine de développement partagée a déjà
 * un autre service sur 4200. reuseExistingServer est désactivé pour ne jamais
 * piloter accidentellement un serveur étranger (cf. docs/adr/0006).
 */
const WEB_PORT = 4201;

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: `http://localhost:${WEB_PORT}`,
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: 'JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -q -f ../backend/gateway/pom.xml spring-boot:run',
      url: 'http://localhost:8082/actuator/health',
      reuseExistingServer: false,
      timeout: 180_000,
    },
    {
      command: `npx ng serve --port ${WEB_PORT}`,
      url: `http://localhost:${WEB_PORT}`,
      reuseExistingServer: false,
      timeout: 120_000,
    },
  ],
});
