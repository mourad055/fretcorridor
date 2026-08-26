import { cleLibelleTenant } from './libelle-tenant';

describe('cleLibelleTenant', () => {
  it('mappe les tenants institutionnels de démonstration vers une clé i18n', () => {
    expect(cleLibelleTenant('tenant-bgft-douala')).toBe('shell.tenant.bgftDouala');
    expect(cleLibelleTenant('tenant-bnft-ndjamena')).toBe('shell.tenant.bnftNdjamena');
    expect(cleLibelleTenant('tenant-flysoft')).toBe('shell.tenant.flysoft');
  });

  it('ne laisse pas MARKETPLACE_CM (tenant chargeur) s afficher comme id brut', () => {
    expect(cleLibelleTenant('MARKETPLACE_CM')).toBe('shell.tenant.marketplace');
  });

  it('retourne null pour un identifiant inconnu (l appelant affiche alors l id)', () => {
    expect(cleLibelleTenant('tenant-inconnu')).toBeNull();
  });
});
