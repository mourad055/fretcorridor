import { Component, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, catchError, combineLatest, map, of, shareReplay, startWith } from 'rxjs';
import { FcResponsiveTableDirective } from '../../../shared/directives/fc-responsive-table.directive';
import { RapportFinancierService } from './rapport-financier.service';
import { Ecriture } from '../../../shared/models/ecriture.models';
import { DeclarationEspeces } from '../../../shared/models/declaration-especes.models';
import { EcrituresTableComponent } from '../../../shared/components/ecritures-table/ecritures-table.component';
import { EspecesTableComponent } from '../../../shared/components/especes-table/especes-table.component';
import { TotauxEcrituresComponent } from '../../../shared/components/totaux-ecritures/totaux-ecritures.component';
import { TotauxEcritures, calculerTotauxEcritures, ecrituresVersCsv, telechargerCsv } from '../../../shared/utils/ecritures-totaux';

interface RapportFinancierVue {
  ecritures: Ecriture[];
  especes: DeclarationEspeces[];
  totaux: TotauxEcritures;
}

/**
 * Rapport financier (Sprint 8, lecture seule) : un Bureau voit les écritures
 * de son territoire.
 *
 * Filtre `missionId` en query param (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.7).
 */
@Component({
  selector: 'app-rapport-financier',
  standalone: true,
  imports: [CommonModule, RouterLink, EcrituresTableComponent, EspecesTableComponent, TotauxEcrituresComponent],
  hostDirectives: [FcResponsiveTableDirective],
  templateUrl: './rapport-financier.component.html',
  styles: `
    :host { display: flex; flex-direction: column; flex: 1 1 auto; width: 100%; min-height: 100%; }
    :host > main { flex: 1 1 auto; width: 100%; min-height: 100%; }
  `,
})
export class RapportFinancierComponent {
  readonly errorMessage = signal<string | null>(null);

  private readonly rapportFinancierService = inject(RapportFinancierService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly missionIdFiltre = toSignal(
    this.route.queryParamMap.pipe(map((params) => params.get('missionId'))),
    { initialValue: this.route.snapshot.queryParamMap.get('missionId') }
  );

  readonly vue$: Observable<RapportFinancierVue> = combineLatest({
    ecritures: this.rapportFinancierService.rapport().pipe(
      catchError(() => {
        this.errorMessage.set('Impossible de charger le rapport financier.');
        return of([] as Ecriture[]);
      })
    ),
    especes: this.rapportFinancierService.paiementsEspeces().pipe(
      catchError(() => of([] as DeclarationEspeces[])),
      startWith([] as DeclarationEspeces[])
    ),
    missionId: toObservable(this.missionIdFiltre),
  }).pipe(
    map(({ ecritures, especes, missionId }) => {
      const liste = missionId ? ecritures.filter((e) => e.missionId === missionId) : ecritures;
      return { ecritures: liste, especes, totaux: calculerTotauxEcritures(liste) };
    }),
    shareReplay(1)
  );

  totauxDe(liste: Ecriture[]) {
    return calculerTotauxEcritures(this.ecrituresAffichees(liste));
  }

  ecrituresAffichees(liste: Ecriture[]): Ecriture[] {
    const missionId = this.missionIdFiltre();
    return missionId ? liste.filter((e) => e.missionId === missionId) : liste;
  }

  exporter(liste: Ecriture[]): void {
    telechargerCsv('rapport-financier-bureau.csv', ecrituresVersCsv(liste));
  }

  reinitialiserFiltre(): void {
    void this.router.navigate([], { queryParams: {} });
  }
}
