import { test, expect, type APIRequestContext } from '@playwright/test';

const SERVICE_ADM_URL = 'http://localhost:8085';

/**
 * Sprint 10 (PRD §9, back-office complet) : un admin traite un dossier de
 * bout en bout — file de travail, prise en charge, décision journalisée —
 * et l'escalade automatique sur dépassement de délai est prouvée en E2E.
 * Les données de service-adm persistent en Postgres entre exécutions
 * (volume Docker) : chaque test ouvre un dossier avec un identifiant de
 * mission unique pour s'identifier dans la file de travail.
 */
async function ouvrirDossier(
  request: APIRequestContext,
  missionId: string,
  delaiTraitement: string
): Promise<void> {
  await request.post(`${SERVICE_ADM_URL}/api/v1/dossiers`, {
    data: {
      tenantId: 'tenant-bgft-douala',
      type: 'LITIGE',
      priorite: 'NORMALE',
      missionId,
      parties: ['acteur-transporteur-1'],
      preuvesReferences: [],
      delaiTraitement,
    },
  });
}

test.describe('Back-office — Admin (Sprint 10)', () => {
  test('un admin traite un dossier de bout en bout, la décision est journalisée', async ({ page, request }) => {
    const missionId = `mission-e2e-${Date.now()}`;
    const delaiFutur = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString();
    await ouvrirDossier(request, missionId, delaiFutur);

    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000003'); // Admin
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/admin$/);
    await page.goto('/admin/dossiers');

    const dossiersSection = page.locator('.dossiers');
    await dossiersSection.getByLabel('Tenant').selectOption('tenant-bgft-douala');
    await dossiersSection.getByRole('button', { name: 'Consulter', exact: true }).click();

    const row = dossiersSection.locator('tbody tr').filter({ hasText: missionId });

    await row.getByRole('button', { name: 'Prendre en charge' }).click();
    await expect(row).toContainText('EN_COURS');

    await row.getByRole('button', { name: 'Voir le dossier' }).click();
    const detail = dossiersSection.locator('.dossier-consolide');
    await expect(detail).toBeVisible();

    await detail.getByLabel('Décision').fill('RESOLU_EN_FAVEUR_TRANSPORTEUR');
    await detail.getByLabel('Motif').fill('Preuve de livraison conforme');
    await detail.getByRole('button', { name: 'Trancher' }).click();

    await expect(row).toContainText('CLOS');
  });

  test('un dossier dont le délai est dépassé est escaladé automatiquement en haute priorité', async ({ page, request }) => {
    const missionId = `mission-e2e-${Date.now()}`;
    const delaiDepasse = new Date(Date.now() - 60 * 60 * 1000).toISOString();
    await ouvrirDossier(request, missionId, delaiDepasse);

    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000003'); // Admin
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/admin$/);
    await page.goto('/admin/dossiers');

    const dossiersSection = page.locator('.dossiers');
    await dossiersSection.getByLabel('Tenant').selectOption('tenant-bgft-douala');
    await dossiersSection.getByRole('button', { name: 'Détecter les dossiers en retard' }).click();
    await dossiersSection.getByRole('button', { name: 'Consulter', exact: true }).click();

    const row = dossiersSection.locator('tbody tr').filter({ hasText: missionId });
    await expect(row).toContainText('ESCALADE');
    await expect(row).toContainText('HAUTE');
  });
});
