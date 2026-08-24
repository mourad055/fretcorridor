export type Role = 'BUREAU' | 'TRANSPORTEUR' | 'ADMIN';

export interface LoginRequest {
  phone: string;
  code: string;
}

export interface LoginResponse {
  token: string;
  role: Role;
  tenantId: string;
}

export interface Session {
  token: string;
  role: Role;
  tenantId: string;
  actorId: string;
}

/** S18 : un tenant sous lequel l'acteur connecté peut opérer (GET /auth/tenants). */
export interface TenantOption {
  tenantId: string;
  origine: boolean;
}

/** Chemin du feature module par rôle — unique source de vérité pour la redirection post-login (FE-WEB-02). */
export const HOME_ROUTE_BY_ROLE: Record<Role, string> = {
  BUREAU: '/bureau',
  TRANSPORTEUR: '/transporteur',
  ADMIN: '/admin',
};
