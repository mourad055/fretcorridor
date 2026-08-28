/**
 * Libellés métier des tenants connus (bandeau, sélection S18).
 * Les identifiants techniques (tenant-bgft-douala, MARKETPLACE_CM) ne sont
 * jamais affichés tels quels quand un nom existe — MARKETPLACE_CM est le
 * tenant du marketplace chargeur (app Client), pas un bureau institutionnel.
 */
export function cleLibelleTenant(tenantId: string): string | null {
  switch (tenantId) {
    case 'tenant-bgft-douala':
      return 'shell.tenant.bgftDouala';
    case 'tenant-bnft-ndjamena':
      return 'shell.tenant.bnftNdjamena';
    case 'tenant-flysoft':
      return 'shell.tenant.flysoft';
    case 'MARKETPLACE_CM':
      return 'shell.tenant.marketplace';
    default:
      return null;
  }
}
