import { test, expect } from '@playwright/test';

/**
 * FE-BUR-01 (Sprint 3) : un Bureau voit une carte des axes de son tenant ;
 * isolation tenant vérifiée (Bureau A ne voit pas les axes du tenant B).
 */
test.describe('Carte des axes — Bureau', () => {
  test('un Bureau voit les axes de son propre tenant', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000001'); // Bureau Douala
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/bureau$/);

    await expect(page.locator('tbody tr')).toHaveCount(2);
    await expect(page.locator('tbody')).toContainText('Douala');
    await expect(page.locator('tbody')).toContainText('Yaoundé');
  });

  test("un Bureau d'un autre tenant ne voit pas les axes du premier", async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+235600000004'); // Bureau Tchad
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/bureau$/);

    await expect(page.locator('tbody tr')).toHaveCount(1);
    await expect(page.locator('tbody')).toContainText("N'Djamena");
    await expect(page.locator('tbody')).not.toContainText('Yaoundé');
    await expect(page.locator('tbody')).not.toContainText('Bafoussam');
  });
});
