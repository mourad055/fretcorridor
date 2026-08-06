import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DossiersService } from './dossiers.service';
import { Dossier, DossierConsolide } from '../../../shared/models/dossier.models';
import { StatusBadgeComponent, dossierStatusVariant } from '../../../shared/components/status-badge/status-badge.component';

/**
 * FE-ADM-01/02 (Sprint 10) : un admin traite un dossier de bout en bout —
 * file de travail priorisée, prise en charge, décision journalisée,
 * dossier consolidé (mission + écritures), escalade automatique sur délai
 * dépassé.
 */
@Component({
  selector: 'app-dossiers',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './dossiers.component.html',
})
export class DossiersComponent {
  readonly dossierStatusVariant = dossierStatusVariant;
  readonly tenants = ['tenant-bgft-douala', 'tenant-bgft-tchad', 'tenant-flysoft'];
  readonly tenantSelectionne = signal(this.tenants[0]);
  readonly dossiers = signal<Dossier[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly dossierConsolide = signal<DossierConsolide | null>(null);
  readonly decisionTexte = signal('');
  readonly motifTexte = signal('');

  constructor(private readonly dossiersService: DossiersService) {}

  consulterFileDeTravail(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.dossiersService.fileDeTravail(this.tenantSelectionne()).subscribe({
      next: (dossiers) => {
        this.dossiers.set(dossiers);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger la file de travail.');
        this.loading.set(false);
      },
    });
  }

  prendreEnCharge(dossierId: string): void {
    this.dossiersService.priseEnCharge(dossierId).subscribe({
      next: () => this.consulterFileDeTravail(),
      error: () => this.errorMessage.set('Impossible de prendre en charge ce dossier.'),
    });
  }

  ouvrirDossier(dossierId: string): void {
    this.dossiersService.consulter(dossierId).subscribe({
      next: (consolide) => this.dossierConsolide.set(consolide),
      error: () => this.errorMessage.set('Impossible de charger le dossier consolidé.'),
    });
  }

  fermerDossier(): void {
    this.dossierConsolide.set(null);
    this.decisionTexte.set('');
    this.motifTexte.set('');
  }

  trancher(): void {
    const consolide = this.dossierConsolide();
    if (!consolide) {
      return;
    }
    this.dossiersService.decider(consolide.dossier.id, this.decisionTexte(), this.motifTexte()).subscribe({
      next: () => {
        this.fermerDossier();
        this.consulterFileDeTravail();
      },
      error: () => this.errorMessage.set('Impossible d\'enregistrer la décision.'),
    });
  }

  declencherEscalade(): void {
    this.dossiersService.declencherEscalade().subscribe({
      next: () => this.consulterFileDeTravail(),
      error: () => this.errorMessage.set('Impossible de déclencher l\'escalade.'),
    });
  }
}
