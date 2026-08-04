import { test, expect } from '@playwright/test';

/**
 * RG-043 (Sprint 6) : un Bureau voit le suivi temps réel de son territoire,
 * chaque position affichant son âge — jamais un horodatage seul.
 */
test.describe('Suivi temps réel — Bureau', () => {
  test('un Bureau voit les positions de son tenant avec leur âge affiché', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000001'); // Bureau Douala
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/bureau$/);

    const positionsSection = page.locator('.positions-list');
    await expect(positionsSection.locator('tbody tr')).toHaveCount(2);
    await expect(positionsSection).toContainText(/il y a|à l'instant/);
  });

  test("un Bureau d'un autre tenant ne voit pas les positions du premier", async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+235600000004'); // Bureau Tchad
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/bureau$/);

    const positionsSection = page.locator('.positions-list');
    await expect(positionsSection.locator('tbody tr')).toHaveCount(1);
    await expect(positionsSection).toContainText('Camion 8T');
  });
});
