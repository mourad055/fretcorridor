import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable, catchError, map, of, shareReplay } from 'rxjs';
import { FcResponsiveTableDirective } from '../../../shared/directives/fc-responsive-table.directive';
import { PaiementService } from './paiement.service';
import { SoldeTransporteur } from './paiement.models';
import { EcrituresTableComponent } from '../../../shared/components/ecritures-table/ecritures-table.component';
import { TotauxEcrituresComponent } from '../../../shared/components/totaux-ecritures/totaux-ecritures.component';
import { calculerTotauxPaiementTransporteur, ecrituresVersCsv, telechargerCsv } from '../../../shared/utils/ecritures-totaux';

@Component({
  selector: 'app-paiement',
  standalone: true,
  imports: [CommonModule, EcrituresTableComponent, TotauxEcrituresComponent],
  hostDirectives: [FcResponsiveTableDirective],
  templateUrl: './paiement.component.html',
  styleUrl: './paiement.component.css',
})
export class PaiementComponent {
  readonly errorMessage = signal<string | null>(null);

  private readonly paiementService = inject(PaiementService);

  readonly solde$: Observable<SoldeTransporteur & { totaux: ReturnType<typeof calculerTotauxPaiementTransporteur> }> =
    this.paiementService.solde().pipe(
    catchError(() => {
      this.errorMessage.set('Impossible de charger votre solde.');
      return of({ solde: 0, historique: [] } as SoldeTransporteur);
    }),
    map((solde) => ({
      ...solde,
      totaux: calculerTotauxPaiementTransporteur(solde.historique ?? []),
    })),
    shareReplay(1)
  );

  exporter(solde: SoldeTransporteur): void {
    telechargerCsv('mes-paiements.csv', ecrituresVersCsv(solde.historique ?? []));
  }
}
