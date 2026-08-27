/** Tenants consultables par l'admin Flysoft (KYC, comptes, audit). */
export const TENANTS_ADMIN = [
  'tenant-bgft-douala',
  'tenant-bnft-ndjamena',
  'tenant-flysoft',
  'MARKETPLACE_CM',
] as const;

export type TenantAdmin = (typeof TENANTS_ADMIN)[number];

export const TENANT_ADMIN_DEFAUT: TenantAdmin = 'tenant-bgft-douala';
