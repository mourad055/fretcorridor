import { test, expect, type APIRequestContext } from '@playwright/test';

const SERVICE_PAY_URL = 'http://localhost:8084';

/**
 * Sprint 8 (PRD §9, cœur du périmètre) : écran solde/historique Transporteur
 * et rapport financier Bureau/Admin, lecture seule, adossés au service-pay
 * réel. Les invariants ENF-FIN-01/02/03 sont déjà prouvés par les tests
 * backend (unitaires + Testcontainers) — ces scénarios E2E couvrent le
 * parcours utilisateur, pas le garde-fou financier lui-même.
 *
 * Les données de service-pay persistent en Postgres entre exécutions de la
 * suite (volume Docker) : chaque test génère un montant unique pour
 * s'identifier dans les tables, sans dépendre d'un total exact.
 */
async function seedMissionPayee(
  request: APIRequestContext,
  missionId: string,
  transporteurId: string,
  montantEncaisse: number,
  montantReverse: number
): Promise<void> {
  await request.post(`${SERVICE_PAY_URL}/api/v1/pay/missions/${missionId}/prise-en-charge`);
  await request.post(`${SERVICE_PAY_URL}/api/v1/pay/missions/${missionId}/cloture`, {
    data: {
      tenantId: 'tenant-bgft-douala',
      transporteurId,
      montant: montantEncaisse,
      referencePrestataire: `ref-${missionId}`,
    },
  });
  await request.post(`${SERVICE_PAY_URL}/api/v1/pay/missions/${missionId}/reversement`, {
    data: {
      tenantId: 'tenant-bgft-douala',
      transporteurId,
      montant: montantReverse,
      referencePrestataire: `ref-rev-${missionId}`,
    },
  });
}

test.describe('Paiement — Sprint 8', () => {
  test("un Transporteur voit un reversement récent dans son solde et son historique", async ({ page, request }) => {
    const missionId = `mission-e2e-${Date.now()}`;
    const montantUnique = 1000 + (Date.now() % 1000);
    await seedMissionPayee(request, missionId, 'actor-transporteur-1', montantUnique, montantUnique);

    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000002'); // Transporteur 1
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/transporteur$/);

    const paiementSection = page.locator('.paiement');
    await expect(paiementSection).toContainText(missionId);
    await expect(paiementSection.locator('tbody')).toContainText(String(montantUnique));
  });

  test("un Bureau voit le rapport financier de son territoire", async ({ page, request }) => {
    const missionId = `mission-e2e-${Date.now()}`;
    const montantUnique = 2000 + (Date.now() % 1000);
    await seedMissionPayee(request, missionId, 'actor-transporteur-1', montantUnique, montantUnique);

    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000001'); // Bureau Douala
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/bureau$/);

    const rapportSection = page.locator('.rapport-financier');
    await expect(rapportSection).toContainText(missionId);
  });

  test("un Admin peut consulter le rapport financier d'un tenant choisi, journalisé", async ({ page, request }) => {
    const missionId = `mission-e2e-${Date.now()}`;
    const montantUnique = 3000 + (Date.now() % 1000);
    await seedMissionPayee(request, missionId, 'actor-transporteur-1', montantUnique, montantUnique);

    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000003'); // Admin
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/admin$/);

    const adminSection = page.locator('.rapport-financier-admin');
    await adminSection.getByLabel('Tenant').selectOption('tenant-bgft-douala');
    await adminSection.getByRole('button', { name: 'Consulter' }).click();

    await expect(adminSection).toContainText(missionId);
  });
});
