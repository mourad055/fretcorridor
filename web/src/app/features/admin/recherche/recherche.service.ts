import { Injectable } from '@angular/core';
import { forkJoin, map, Observable } from 'rxjs';
import { TenantsService } from '../tenants/tenants.service';
import { JournalAuditService } from '../journal-audit/journal-audit.service';
import { ResultatRecherche } from './recherche.models';

function contient(champ: string | null | undefined, terme: string): boolean {
  return (champ ?? '').toLowerCase().includes(terme);
}

/**
 * Recherche globale transverse (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.5) : un Admin qui
 * supervise tous les tenants n'avait aucun moyen de retrouver un tenant ou
 * une action du journal d'audit sans savoir à l'avance dans quel écran
 * chercher. S'appuie exclusivement sur des endpoints déjà réels — Tenants
 * (liste complète) et Journal d'audit (ADMINISTRATION, tenantId omis =
 * tous les tenants) — aucun nouvel endpoint, filtrage côté client.
 */
@Injectable({ providedIn: 'root' })
export class RechercheService {
  constructor(
    private readonly tenantsService: TenantsService,
    private readonly journalAuditService: JournalAuditService
  ) {}

  rechercher(terme: string): Observable<ResultatRecherche[]> {
    const termeNormalise = terme.trim().toLowerCase();
    return forkJoin({
      tenants: this.tenantsService.lister(),
      journal: this.journalAuditService.lister(),
    }).pipe(
      map(({ tenants, journal }) => {
        const resultatsTenants: ResultatRecherche[] = tenants
          .filter(
            (t) =>
              contient(t.id, termeNormalise) || contient(t.nom, termeNormalise) || contient(t.pays, termeNormalise)
          )
          .map((t) => ({
            type: 'TENANT' as const,
            titre: t.nom,
            detail: `${t.id} — ${t.pays} — ${t.actif ? 'actif' : 'inactif'}`,
            tenantId: t.id,
          }));

        const resultatsJournal: ResultatRecherche[] = journal
          .filter(
            (e) =>
              contient(e.acteurId, termeNormalise) ||
              contient(e.action, termeNormalise) ||
              contient(e.ressource, termeNormalise) ||
              contient(e.tenantId, termeNormalise)
          )
          .map((e) => ({
            type: 'JOURNAL_AUDIT' as const,
            titre: e.action,
            detail: `${e.ressource} — ${e.acteurId} — ${e.horodatage}`,
            tenantId: e.tenantId,
          }));

        return [...resultatsTenants, ...resultatsJournal];
      })
    );
  }
}
