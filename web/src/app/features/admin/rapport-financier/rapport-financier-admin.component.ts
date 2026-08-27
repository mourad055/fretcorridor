import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BehaviorSubject, Observable, catchError, combineLatest, map, of, shareReplay, startWith, switchMap } from 'rxjs';
import { FcResponsiveTableDirective } from '../../../shared/directives/fc-responsive-table.directive';
import { RapportFinancierAdminService } from './rapport-financier-admin.service';
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

@Component({
  selector: 'app-rapport-financier-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, EcrituresTableComponent, EspecesTableComponent, TotauxEcrituresComponent],
  hostDirectives: [FcResponsiveTableDirective],
  templateUrl: './rapport-financier-admin.component.html',
  styles: `
    :host { display: flex; flex-direction: column; flex: 1 1 auto; width: 100%; min-height: 100%; }
    :host > main { flex: 1 1 auto; width: 100%; min-height: 100%; }
  `,
})
export class RapportFinancierAdminComponent {
  readonly tenants = ['tenant-bgft-douala', 'tenant-bnft-ndjamena', 'tenant-flysoft'];
  readonly tenantSelectionne = signal(this.tenants[0]);
  readonly errorMessage = signal<string | null>(null);

  private readonly rapportFinancierAdminService = inject(RapportFinancierAdminService);
  private readonly consultation$ = new BehaviorSubject<string>(this.tenants[0]);

  readonly vue$: Observable<RapportFinancierVue> = this.consultation$.pipe(
    switchMap((tenantId) =>
      combineLatest({
        ecritures: this.rapportFinancierAdminService.rapport(tenantId).pipe(
          catchError(() => {
            this.errorMessage.set('Impossible de charger le rapport financier de ce tenant.');
            return of([] as Ecriture[]);
          })
        ),
        especes: this.rapportFinancierAdminService.paiementsEspeces(tenantId).pipe(
          catchError(() => of([] as DeclarationEspeces[])),
          startWith([] as DeclarationEspeces[])
        ),
      })
    ),
    map((vue) => ({ ...vue, totaux: calculerTotauxEcritures(vue.ecritures) })),
    shareReplay(1)
  );

  totauxDe(liste: Ecriture[]) {
    return calculerTotauxEcritures(liste);
  }

  consulter(): void {
    this.errorMessage.set(null);
    this.consultation$.next(this.tenantSelectionne());
  }

  exporter(liste: Ecriture[]): void {
    telechargerCsv(`rapport-financier-${this.tenantSelectionne()}.csv`, ecrituresVersCsv(liste));
  }
}
