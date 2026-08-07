import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KycService } from './kyc.service';
import { KycDossier } from './kyc.models';

/**
 * FE-ADM-06 : un admin voit une liste de KYC en attente et peut la faire
 * passer à validé/rejeté. Onglet « KYC » du tableau de bord Admin — voir
 * docs/adr, Sprint 14 (navigation par onglets).
 */
@Component({
  selector: 'app-kyc-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kyc-dashboard.component.html',
})
export class KycDashboardComponent implements OnInit {
  readonly dossiers = signal<KycDossier[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly dossierEnCours = signal<string | null>(null);

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
