import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PaiementService } from './paiement.service';
import { SoldeTransporteur } from './paiement.models';
import { EcrituresTableComponent } from '../../../shared/components/ecritures-table/ecritures-table.component';

/** FE-TRP-03 (Sprint 8) : un Transporteur voit son solde et son historique de paiement. */
@Component({
  selector: 'app-paiement',
  standalone: true,
  imports: [CommonModule, EcrituresTableComponent],
  templateUrl: './paiement.component.html',
})
export class PaiementComponent implements OnInit {
  readonly solde = signal<SoldeTransporteur | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly paiementService: PaiementService) {}

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
