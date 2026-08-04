import { test, expect } from '@playwright/test';

/**
 * FE-TRP-01 (Sprint 4) : un Transporteur voit ses capacités déclarées ; aucune
 * capacité d'un autre transporteur n'est visible (PRD §5.3).
 */
test.describe('Capacités déclarées — Transporteur', () => {
  test('un Transporteur voit ses propres capacités', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000002'); // Transporteur 1
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/transporteur$/);

    await expect(page.locator('tbody tr')).toHaveCount(2);
    await expect(page.locator('tbody')).toContainText('Camion 10T');
  });

  test("un second Transporteur ne voit pas les capacités du premier", async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000005'); // Transporteur 2
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/transporteur$/);

    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody')).toContainText('Fourgon 3T');
    await expect(page.locator('tbody')).not.toContainText('Camion 10T');
  });
});
