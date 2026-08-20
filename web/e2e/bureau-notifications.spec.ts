import { test, expect } from '@playwright/test';

/**
 * PRD §9 S9 : centre de notifications côté web (canal email). Un Bureau ne
 * voit que les notifications de son propre tenant (ENF-MUL-01).
 */
test.describe('Centre de notifications — Bureau', () => {
  test('un Bureau voit ses notifications email dans le centre web', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000001'); // Bureau Douala
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/bureau$/);
    await page.goto('/bureau/notifications');

    const notifications = page.locator('.notifications');
    await expect(notifications.locator('li')).toHaveCount(2);
    await expect(notifications).toContainText('Nouvelle mission appariée');
    await expect(notifications).toContainText('EMAIL');
  });

  test("un Bureau d'un autre tenant ne voit pas les notifications du premier", async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+235600000004'); // Bureau Tchad
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/bureau$/);
    await page.goto('/bureau/notifications');

    const notifications = page.locator('.notifications');
    await expect(notifications.locator('li')).toHaveCount(1);
    await expect(notifications).toContainText('Dossier KYC validé');
    await expect(notifications).not.toContainText('Nouvelle mission appariée');
  });
});
