import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RapportFinancierAdminService } from './rapport-financier-admin.service';
import { Ecriture } from '../../../shared/models/ecriture.models';
import { EcrituresTableComponent } from '../../../shared/components/ecritures-table/ecritures-table.component';

/**
 * Rapport financier Admin (Sprint 8) : consultation transverse à tous les
 * tenants — chaque consultation est journalisée nominativement côté gateway
 * (ENF-SEC-02), jamais silencieuse.
 */
@Component({
  selector: 'app-rapport-financier-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, EcrituresTableComponent],
  templateUrl: './rapport-financier-admin.component.html',
})
export class RapportFinancierAdminComponent {
  readonly tenants = ['tenant-bgft-douala', 'tenant-bgft-tchad', 'tenant-flysoft'];
  readonly tenantSelectionne = signal(this.tenants[0]);
  readonly ecritures = signal<Ecriture[] | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly rapportFinancierAdminService: RapportFinancierAdminService) {}

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
  }
}
