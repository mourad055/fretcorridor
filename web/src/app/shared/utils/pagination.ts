/**
 * Pagination généralisée (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §3.3) : aucun tableau du
 * produit n'était paginé — un tenant/journal d'audit avec beaucoup d'entrées
 * rendait la totalité en une seule liste. Pagination purement côté client
 * (les endpoints renvoient déjà la liste complète, aucun changement serveur).
 */
export function paginer<T>(items: T[], page: number, taillePage: number): T[] {
  const debut = (page - 1) * taillePage;
  return items.slice(debut, debut + taillePage);
}

export function nombreDePages(total: number, taillePage: number): number {
  return Math.max(1, Math.ceil(total / taillePage));
}
