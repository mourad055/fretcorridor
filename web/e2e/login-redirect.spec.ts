import { test, expect } from '@playwright/test';

/**
 * FE-WEB-02 : après connexion, résolution du rôle depuis le JWT et redirection
 * vers le feature module correspondant. Un scénario par rôle démontré (Bureau) —
 * les deux autres suivent le même mécanisme (guard partagé, cf. role.guard.spec.ts).
 */
test.describe('Connexion et redirection par rôle', () => {
  test('un Bureau de fret authentifié est redirigé vers /bureau', async ({ page }) => {
    await page.goto('/login');

    await page.getByLabel('Numéro de téléphone').fill('+237600000001');
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();

    await expect(page).toHaveURL(/\/bureau$/);
    await expect(page.getByRole('heading', { name: 'Carte des axes' })).toBeVisible();
  });

  test('un Transporteur authentifié est redirigé vers /transporteur', async ({ page }) => {
    await page.goto('/login');

    await page.getByLabel('Numéro de téléphone').fill('+237600000002');
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();

    await expect(page).toHaveURL(/\/transporteur$/);
    await expect(page.getByRole('heading', { name: 'Transporteur' })).toBeVisible();
  });

  test('des identifiants invalides affichent une erreur explicite, sans redirection', async ({ page }) => {
    await page.goto('/login');

    await page.getByLabel('Numéro de téléphone').fill('+237699999999');
    await page.getByLabel('Code reçu par SMS').fill('000000');
    await page.getByRole('button', { name: 'Se connecter' }).click();

    await expect(page.getByRole('alert')).toHaveText(/invalide/i);
    await expect(page).toHaveURL(/\/login$/);
  });
});
