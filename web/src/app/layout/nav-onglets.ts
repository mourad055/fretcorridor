import { Role } from '../core/auth/auth.models';

export interface OngletNav {
  path: string;
  labelKey: string;
  exact: boolean;
  /** Regroupement optionnel (sidebar uniquement) — absent = pas de groupe (rendu à plat). */
  groupeKey?: string;
}

/**
 * Source unique des onglets par rôle (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.1) : jusqu'ici définis
 * uniquement dans ShellNavComponent (nav horizontale) — ShellSidebarComponent
 * (nav verticale desktop) consomme désormais la même liste, pour ne jamais
 * diverger entre les deux présentations d'une même navigation. Ordre inchangé
 * par rapport à la nav horizontale existante — seul `groupeKey` est ajouté,
 * consommé uniquement par la sidebar (le groupement n'affecte pas l'ordre
 * des pilules horizontales).
 */
const ONGLETS_BUREAU: OngletNav[] = [
  { path: '/bureau', labelKey: 'nav.bureau.axes', exact: true, groupeKey: 'nav.groupe.supervision' },
  { path: '/bureau/missions', labelKey: 'nav.bureau.missions', exact: false, groupeKey: 'nav.groupe.exploitation' },
  { path: '/bureau/positions', labelKey: 'nav.bureau.positions', exact: false, groupeKey: 'nav.groupe.supervision' },
  { path: '/bureau/chronologie', labelKey: 'nav.bureau.chronologie', exact: false, groupeKey: 'nav.groupe.supervision' },
  { path: '/bureau/rapport-financier', labelKey: 'nav.bureau.rapportFinancier', exact: false, groupeKey: 'nav.groupe.exploitation' },
  { path: '/bureau/notifications', labelKey: 'nav.bureau.notifications', exact: false, groupeKey: 'nav.groupe.exploitation' },
  { path: '/bureau/observatoire', labelKey: 'nav.bureau.observatoire', exact: false, groupeKey: 'nav.groupe.supervision' },
];

const ONGLETS_TRANSPORTEUR: OngletNav[] = [
  { path: '/transporteur', labelKey: 'nav.transporteur.capacites', exact: true },
  { path: '/transporteur/missions', labelKey: 'nav.transporteur.missions', exact: false },
  { path: '/transporteur/paiement', labelKey: 'nav.transporteur.paiement', exact: false },
];

const ONGLETS_ADMIN: OngletNav[] = [
  { path: '/admin', labelKey: 'nav.admin.kyc', exact: true, groupeKey: 'nav.groupe.conformite' },
  { path: '/admin/rapport-financier', labelKey: 'nav.admin.rapportFinancier', exact: false, groupeKey: 'nav.groupe.finance' },
  { path: '/admin/dossiers', labelKey: 'nav.admin.dossiers', exact: false, groupeKey: 'nav.groupe.conformite' },
  { path: '/admin/configurations', labelKey: 'nav.admin.configuration', exact: false, groupeKey: 'nav.groupe.configuration' },
  { path: '/admin/tenants', labelKey: 'nav.admin.tenants', exact: false, groupeKey: 'nav.groupe.configuration' },
  { path: '/admin/journal-audit', labelKey: 'nav.admin.journalAudit', exact: false, groupeKey: 'nav.groupe.conformite' },
  { path: '/admin/comptes', labelKey: 'nav.admin.comptes', exact: false, groupeKey: 'nav.groupe.configuration' },
  { path: '/admin/recherche', labelKey: 'nav.admin.recherche', exact: false },
];

export function ongletsPourRole(role: Role | undefined): OngletNav[] {
  switch (role) {
    case 'BUREAU':
      return ONGLETS_BUREAU;
    case 'TRANSPORTEUR':
      return ONGLETS_TRANSPORTEUR;
    case 'ADMIN':
      return ONGLETS_ADMIN;
    default:
      return [];
  }
}
