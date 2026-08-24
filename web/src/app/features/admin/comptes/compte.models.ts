/** Rôles portés par un Acteur (service-ida, source d'identité unique — mobile + web). */
export type RoleActeur =
  | 'CHAUFFEUR'
  | 'TRANSPORTEUR'
  | 'CHAUFFEUR_PROPRIETAIRE'
  | 'AGENT'
  | 'CHARGEUR'
  | 'BUREAU'
  | 'ADMINISTRATION';

export const ROLES_ACTEUR: RoleActeur[] = [
  'CHAUFFEUR',
  'TRANSPORTEUR',
  'CHAUFFEUR_PROPRIETAIRE',
  'AGENT',
  'CHARGEUR',
  'BUREAU',
  'ADMINISTRATION',
];

export interface CompteAdmin {
  id: string;
  telephone: string;
  nom: string | null;
  prenom: string | null;
  raisonSociale: string | null;
  tenantId: string;
  roles: RoleActeur[];
  actif: boolean;
  niveauKyc: string;
}
