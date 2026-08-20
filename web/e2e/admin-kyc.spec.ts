import { test, expect, type Page } from '@playwright/test';

/**
 * FE-ADM-06 (Sprint 2) : un admin voit une liste de KYC en attente et peut la
 * faire passer à un état validé/rejeté.
 *
 * L'adaptateur mock du gateway garde son état en mémoire pour toute la durée
 * du process — partagé par tous les specs E2E de ce run. Les assertions se
 * basent donc sur une variation de compte (avant/après), jamais sur un total
 * fixe, pour rester indépendantes de l'ordre d'exécution.
 */
async function waitForDashboardLoaded(page: Page): Promise<void> {
  await expect(page.getByText('Chargement…')).toHaveCount(0);
}

test.describe('Dashboard KYC Admin', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000003'); // Admin
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/admin$/);
    await waitForDashboardLoaded(page);
  });

  test("un admin voit la file d'attente et peut valider un dossier", async ({ page }) => {
    const kycTable = page.locator('table.kyc-table');
    const rows = kycTable.locator('tbody tr');
    const initialCount = await rows.count();
    expect(initialCount).toBeGreaterThan(0);

    const firstRow = rows.first();
    const acteurNom = await firstRow.locator('td').first().textContent();
    await firstRow.getByRole('button', { name: 'Valider' }).click();

    await expect(rows).toHaveCount(initialCount - 1);
    await expect(kycTable.locator('tbody')).not.toContainText(acteurNom ?? '');
  });

  test('un admin peut rejeter un dossier', async ({ page }) => {
    const kycTable = page.locator('table.kyc-table');
    const rows = kycTable.locator('tbody tr');
    const initialCount = await rows.count();
    expect(initialCount).toBeGreaterThan(0);

    await rows.first().getByRole('button', { name: 'Rejeter' }).click();

    await expect(rows).toHaveCount(initialCount - 1);
  });
});
