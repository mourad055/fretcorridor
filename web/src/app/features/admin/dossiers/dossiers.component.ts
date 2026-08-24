import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { DossiersService } from './dossiers.service';
import { Dossier, DossierConsolide } from '../../../shared/models/dossier.models';
import { ConfirmationService } from '../../../shared/services/confirmation.service';
import {
  StatusBadgeComponent,
  dossierStatusVariant,
  libelleDossierStatut,
  libelleTypeDossier,
  libellePrioriteDossier,
  libelleEtapeEtat,
} from '../../../shared/components/status-badge/status-badge.component';

/**
 * FE-ADM-01/02 (Sprint 10) : un admin traite un dossier de bout en bout —
 * file de travail priorisée, prise en charge, décision journalisée,
 * dossier consolidé (mission + écritures), escalade automatique sur délai
 * dépassé.
 */
@Component({
  selector: 'app-dossiers',
  standalone: true,
  imports: [CommonModule, PageShellComponent, FormsModule, StatusBadgeComponent, TranslatePipe],
  templateUrl: './dossiers.component.html',
})
export class DossiersComponent {
  readonly dossierStatusVariant = dossierStatusVariant;
  readonly libelleDossierStatut = libelleDossierStatut;
  readonly libelleTypeDossier = libelleTypeDossier;
  readonly libellePrioriteDossier = libellePrioriteDossier;
  readonly libelleEtapeEtat = libelleEtapeEtat;
  readonly tenants = ['tenant-bgft-douala', 'tenant-bnft-ndjamena', 'tenant-flysoft'];
  readonly tenantSelectionne = signal(this.tenants[0]);
  readonly dossiers = signal<Dossier[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly dossierConsolide = signal<DossierConsolide | null>(null);
  readonly decisionTexte = signal('');
  readonly motifTexte = signal('');
  readonly dossierEnCours = signal<string | null>(null);
  readonly trancheEnCours = signal(false);
  readonly escaladeEnCours = signal(false);
  readonly decisionValide = computed(() => this.decisionTexte().trim().length > 0 && this.motifTexte().trim().length > 0);

  /** KPIs en tête d'écran (audit UX 2026-08-23, §1.6). */
  readonly nombreEnAttente = computed(() => this.dossiers().filter((d) => d.statut !== 'CLOS').length);
  readonly nombreEnRetard = computed(() => {
    const maintenant = Date.now();
    return this.dossiers().filter((d) => d.statut !== 'CLOS' && new Date(d.delaiTraitement).getTime() < maintenant).length;
  });

  constructor(
    private readonly dossiersService: DossiersService,
    private readonly confirmationService: ConfirmationService
  ) {}

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
    this.dossierEnCours.set(dossierId);
    this.dossiersService.priseEnCharge(dossierId).subscribe({
      next: () => {
        this.dossierEnCours.set(null);
        this.consulterFileDeTravail();
      },
      error: () => {
        this.errorMessage.set('Impossible de prendre en charge ce dossier.');
        this.dossierEnCours.set(null);
      },
    });
  }

  ouvrirDossier(dossierId: string): void {
    this.dossierEnCours.set(dossierId);
    this.dossiersService.consulter(dossierId).subscribe({
      next: (consolide) => {
        this.dossierConsolide.set(consolide);
        this.dossierEnCours.set(null);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le dossier consolidé.');
        this.dossierEnCours.set(null);
      },
    });
  }

  fermerDossier(): void {
    this.dossierConsolide.set(null);
    this.decisionTexte.set('');
    this.motifTexte.set('');
  }

  trancher(): void {
    const consolide = this.dossierConsolide();
    if (!consolide || !this.decisionValide()) {
      return;
    }
    const confirme = this.confirmationService.confirmer(
      `Trancher le dossier ${consolide.dossier.id} avec la décision « ${this.decisionTexte()} » ? Cette décision est définitive.`
    );
    if (!confirme) {
      return;
    }
    this.trancheEnCours.set(true);
    this.dossiersService.decider(consolide.dossier.id, this.decisionTexte(), this.motifTexte()).subscribe({
      next: () => {
        this.trancheEnCours.set(false);
        this.fermerDossier();
        this.consulterFileDeTravail();
      },
      error: () => {
        this.errorMessage.set('Impossible d\'enregistrer la décision.');
        this.trancheEnCours.set(false);
      },
    });
  }

  declencherEscalade(): void {
    this.escaladeEnCours.set(true);
    this.dossiersService.declencherEscalade().subscribe({
      next: () => {
        this.escaladeEnCours.set(false);
        this.consulterFileDeTravail();
      },
      error: () => {
        this.errorMessage.set('Impossible de déclencher l\'escalade.');
        this.escaladeEnCours.set(false);
      },
    });
  }
}
