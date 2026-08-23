import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { PaiementService } from './paiement.service';
import { SoldeTransporteur } from './paiement.models';
import { EcrituresTableComponent } from '../../../shared/components/ecritures-table/ecritures-table.component';
import { TotauxEcrituresComponent } from '../../../shared/components/totaux-ecritures/totaux-ecritures.component';
import { calculerTotauxEcritures, ecrituresVersCsv, telechargerCsv } from '../../../shared/utils/ecritures-totaux';

/** FE-TRP-03 (Sprint 8) : un Transporteur voit son solde et son historique de paiement. */
@Component({
  selector: 'app-paiement',
  standalone: true,
  imports: [CommonModule, PageShellComponent, EcrituresTableComponent, TotauxEcrituresComponent],
  templateUrl: './paiement.component.html',
  styleUrl: './paiement.component.css',
})
export class PaiementComponent implements OnInit {
  readonly solde = signal<SoldeTransporteur | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly totaux = computed(() => calculerTotauxEcritures(this.solde()?.historique ?? []));

  constructor(private readonly paiementService: PaiementService) {}

  exporter(): void {
    const historique = this.solde()?.historique;
    if (!historique) {
      return;
    }
    telechargerCsv('mes-paiements.csv', ecrituresVersCsv(historique));
  }

  ngOnInit(): void {
    this.paiementService.solde().subscribe({
      next: (solde) => {
        this.solde.set(solde);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger votre solde.');
        this.loading.set(false);
      },
    });
  }
}
