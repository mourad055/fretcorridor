import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { RapportFinancierService } from './rapport-financier.service';
import { Ecriture } from '../../../shared/models/ecriture.models';
import { DeclarationEspeces } from '../../../shared/models/declaration-especes.models';
import { EcrituresTableComponent } from '../../../shared/components/ecritures-table/ecritures-table.component';
import { EspecesTableComponent } from '../../../shared/components/especes-table/especes-table.component';

/** Rapport financier (Sprint 8, lecture seule) : un Bureau voit les écritures de son territoire. */
@Component({
  selector: 'app-rapport-financier',
  standalone: true,
  imports: [CommonModule, PageShellComponent, EcrituresTableComponent, EspecesTableComponent],
  templateUrl: './rapport-financier.component.html',
})
export class RapportFinancierComponent implements OnInit {
  readonly ecritures = signal<Ecriture[]>([]);
  readonly paiementsEspeces = signal<DeclarationEspeces[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly rapportFinancierService: RapportFinancierService) {}

  ngOnInit(): void {
    this.rapportFinancierService.rapport().subscribe({
      next: (ecritures) => {
        this.ecritures.set(ecritures);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le rapport financier.');
        this.loading.set(false);
      },
    });

    this.rapportFinancierService.paiementsEspeces().subscribe({
      next: (paiements) => this.paiementsEspeces.set(paiements),
      error: () => this.paiementsEspeces.set([]),
    });
  }
}
