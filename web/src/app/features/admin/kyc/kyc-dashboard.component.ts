import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { TranslatePipe } from '@ngx-translate/core';
import { KycService } from './kyc.service';
import { KycDossier } from './kyc.models';
import { libelleTypeActeur } from '../../../shared/components/status-badge/status-badge.component';

/**
 * FE-ADM-06 : un admin voit une liste de KYC en attente et peut la faire
 * passer à validé/rejeté. Onglet « KYC » du tableau de bord Admin — voir
 * docs/adr, Sprint 14 (navigation par onglets).
 */
@Component({
  selector: 'app-kyc-dashboard',
  standalone: true,
  imports: [CommonModule, PageShellComponent, TranslatePipe],
  templateUrl: './kyc-dashboard.component.html',
})
export class KycDashboardComponent implements OnInit {
  readonly dossiers = signal<KycDossier[]>([]);
  /** KPI en tête d'écran (audit UX 2026-08-23, §1.6) : le nombre en attente était déjà visible en comptant les lignes, jamais affiché en chiffre. */
  readonly nombreEnAttente = computed(() => this.dossiers().length);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly dossierEnCours = signal<string | null>(null);
  readonly libelleTypeActeur = libelleTypeActeur;

  constructor(private readonly kycService: KycService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.kycService.listPending().subscribe({
      next: (dossiers) => {
        this.dossiers.set(dossiers);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger la file de vérification KYC.');
        this.loading.set(false);
      },
    });
  }

  decide(dossier: KycDossier, decision: 'VALIDE' | 'REJETE'): void {
    this.dossierEnCours.set(dossier.id);
    this.kycService.decide(dossier.id, decision).subscribe({
      next: () => {
        this.dossiers.update((current) => current.filter((d) => d.id !== dossier.id));
        this.dossierEnCours.set(null);
      },
      error: () => {
        this.errorMessage.set(`La décision sur le dossier de ${dossier.acteurNom} a échoué.`);
        this.dossierEnCours.set(null);
      },
    });
  }
}
