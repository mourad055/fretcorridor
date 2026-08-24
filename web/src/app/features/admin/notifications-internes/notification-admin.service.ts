import { Injectable } from '@angular/core';
import { catchError, forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { TenantsService } from '../tenants/tenants.service';
import { DossiersService } from '../dossiers/dossiers.service';
import { NotificationAdmin } from './notification-admin.models';

/**
 * Centre de notifications internes Admin (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.6) : le Bureau a un
 * centre de notifications (Sprint 9), rien d'équivalent n'existait côté
 * Admin — pourtant c'est le rôle qui reçoit le plus d'alertes système
 * transverses. Construit uniquement à partir de données déjà réelles
 * (dossiers en retard, tous tenants confondus) — pas de nouvel état
 * persistant, pas de notion de "lu/non lu" inventée sans support backend.
 */
@Injectable({ providedIn: 'root' })
export class NotificationAdminService {
  constructor(
    private readonly tenantsService: TenantsService,
    private readonly dossiersService: DossiersService
  ) {}

  dossiersEnRetard(): Observable<NotificationAdmin[]> {
    return this.tenantsService.lister().pipe(
      switchMap((tenants) =>
        forkJoin(
          tenants.map((tenant) =>
            this.dossiersService.fileDeTravail(tenant.id).pipe(
              map((dossiers) => ({ tenant, dossiers })),
              catchError(() => of({ tenant, dossiers: [] }))
            )
          )
        )
      ),
      map((parTenant) => {
        const maintenant = Date.now();
        const alertes: NotificationAdmin[] = [];
        for (const { tenant, dossiers } of parTenant) {
          for (const dossier of dossiers) {
            const enRetard = dossier.statut !== 'CLOS' && new Date(dossier.delaiTraitement).getTime() < maintenant;
            if (enRetard) {
              alertes.push({
                titre: `Dossier en retard — ${tenant.nom}`,
                detail: `${dossier.type} (${dossier.priorite}) — délai dépassé le ${dossier.delaiTraitement}`,
                tenantId: tenant.id,
                dossierId: dossier.id,
              });
            }
          }
        }
        return alertes;
      })
    );
  }
}
