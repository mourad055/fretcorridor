import { test, expect } from '@playwright/test';

/**
 * FE-BUR-01 (Sprint 5) : un Bureau voit les missions appariées de son
 * territoire ; isolation tenant vérifiée (comme pour les axes, Sprint 3).
 */
test.describe('Missions appariées — Bureau', () => {
  test('un Bureau voit les missions appariées de son propre tenant', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000001'); // Bureau Douala
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/bureau$/);
    await page.goto('/bureau/missions');

    const missionsSection = page.locator('.missions-list');
    await expect(missionsSection.locator('tbody tr')).toHaveCount(2);
    await expect(missionsSection).toContainText('Transport Étoile SARL');
  });

  test("un Bureau d'un autre tenant ne voit pas les missions du premier", async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+235600000004'); // Bureau Tchad
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/bureau$/);
    await page.goto('/bureau/missions');

    const missionsSection = page.locator('.missions-list');
    await expect(missionsSection.locator('tbody tr')).toHaveCount(1);
    await expect(missionsSection).toContainText('Transporteur Sahel');
    await expect(missionsSection).not.toContainText('Transport Étoile SARL');
  });
});
