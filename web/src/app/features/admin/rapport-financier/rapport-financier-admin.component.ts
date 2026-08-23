import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { FormsModule } from '@angular/forms';
import { RapportFinancierAdminService } from './rapport-financier-admin.service';
import { Ecriture } from '../../../shared/models/ecriture.models';
import { DeclarationEspeces } from '../../../shared/models/declaration-especes.models';
import { EcrituresTableComponent } from '../../../shared/components/ecritures-table/ecritures-table.component';
import { EspecesTableComponent } from '../../../shared/components/especes-table/especes-table.component';
import { TotauxEcrituresComponent } from '../../../shared/components/totaux-ecritures/totaux-ecritures.component';
import { calculerTotauxEcritures, ecrituresVersCsv, telechargerCsv } from '../../../shared/utils/ecritures-totaux';

/**
 * Rapport financier Admin (Sprint 8) : consultation transverse à tous les
 * tenants — chaque consultation est journalisée nominativement côté gateway
 * (ENF-SEC-02), jamais silencieuse.
 */
@Component({
  selector: 'app-rapport-financier-admin',
  standalone: true,
  imports: [CommonModule, PageShellComponent, FormsModule, EcrituresTableComponent, EspecesTableComponent, TotauxEcrituresComponent],
  templateUrl: './rapport-financier-admin.component.html',
})
export class RapportFinancierAdminComponent {
  readonly tenants = ['tenant-bgft-douala', 'tenant-bnft-ndjamena', 'tenant-flysoft'];
  readonly tenantSelectionne = signal(this.tenants[0]);
  readonly ecritures = signal<Ecriture[] | null>(null);
  readonly paiementsEspeces = signal<DeclarationEspeces[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly totaux = computed(() => calculerTotauxEcritures(this.ecritures() ?? []));

  constructor(private readonly rapportFinancierAdminService: RapportFinancierAdminService) {}

  exporter(): void {
    const ecritures = this.ecritures();
    if (!ecritures) {
      return;
    }
    telechargerCsv(`rapport-financier-${this.tenantSelectionne()}.csv`, ecrituresVersCsv(ecritures));
  }

  consulter(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.rapportFinancierAdminService.rapport(this.tenantSelectionne()).subscribe({
      next: (ecritures) => {
        this.ecritures.set(ecritures);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le rapport financier de ce tenant.');
        this.loading.set(false);
      },
    });

    this.rapportFinancierAdminService.paiementsEspeces(this.tenantSelectionne()).subscribe({
      next: (paiements) => this.paiementsEspeces.set(paiements),
      error: () => this.paiementsEspeces.set([]),
    });
  }
}
