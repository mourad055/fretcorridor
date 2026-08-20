import { test, expect } from '@playwright/test';

/**
 * Sprint 7 (PRD §9) : un Transporteur ne voit que ses missions ; un Bureau
 * voit celles de son territoire.
 */
test.describe('Chronologie de mission', () => {
  test('un Bureau voit la chronologie des missions de son territoire', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000001'); // Bureau Douala
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/bureau$/);
    await page.goto('/bureau/chronologie');

    const chronologie = page.locator('.bureau-chronologie');
    await expect(chronologie.locator('app-mission-chronologie article')).toHaveCount(2);
  });

  test('un Transporteur voit uniquement ses propres missions', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000002'); // Transporteur 1
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/transporteur$/);
    await page.goto('/transporteur/missions');

    const mesMissions = page.locator('.transporteur-missions');
    await expect(mesMissions.locator('app-mission-chronologie article')).toHaveCount(1);
    await expect(mesMissions).toContainText('Transport Étoile SARL');
  });

  test("un second Transporteur ne voit pas la mission du premier", async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000005'); // Transporteur 2
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/transporteur$/);
    await page.goto('/transporteur/missions');

    const mesMissions = page.locator('.transporteur-missions');
    await expect(mesMissions.locator('app-mission-chronologie article')).toHaveCount(1);
    await expect(mesMissions).not.toContainText('Transport Étoile SARL');
  });
});
