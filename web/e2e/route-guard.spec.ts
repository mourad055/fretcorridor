import { test, expect } from '@playwright/test';

/**
 * FE-WEB-03 / FE-WEB-04 : aucune route n'est atteignable en modifiant l'URL
 * manuellement sans passer la garde de rôle. La réponse est un 403 propre,
 * jamais une erreur technique, et aucune requête vers la ressource protégée
 * n'est déclenchée (vérifié par interception réseau).
 */
test.describe('Garde de routes RBAC', () => {
  test('un accès direct à une route hors rôle redirige vers /403 sans requête réseau vers la ressource', async ({
    page,
  }) => {
    await page.goto('/login');
    await page.getByLabel('Numéro de téléphone').fill('+237600000002'); // Transporteur
    await page.getByLabel('Code reçu par SMS').fill('123456');
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/\/transporteur$/);

    const adminRequests: string[] = [];
    page.on('request', (request) => {
      if (request.url().includes('/api/v1/admin/')) {
        adminRequests.push(request.url());
      }
    });

    await page.goto('/admin');

    await expect(page).toHaveURL(/\/403$/);
    await expect(page.getByRole('heading', { name: 'Accès non autorisé' })).toBeVisible();
    expect(adminRequests).toHaveLength(0);
  });

  test('un accès direct sans authentification redirige vers /login', async ({ page }) => {
    await page.goto('/bureau');

    await expect(page).toHaveURL(/\/login$/);
  });
});
