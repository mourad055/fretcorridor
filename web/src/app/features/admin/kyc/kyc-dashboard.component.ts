import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { TranslatePipe } from '@ngx-translate/core';
import { paginer } from '../../../shared/utils/pagination';
import { ConfirmationService } from '../../../shared/services/confirmation.service';
import { KycService } from './kyc.service';
import { KycDetail, KycDossier, KycFiltre } from './kyc.models';
import { libelleTypeActeur } from '../../../shared/components/status-badge/status-badge.component';

const TAILLE_PAGE = 20;

/**
 * FE-ADM-06 : file de revue KYC réelle (IDA) — filtres En attente / N1 / N2,
 * détail pièces présignées, valider/rejeter. Pas de CRUD créer/supprimer acteur
 * (lien secondaire vers /admin/comptes).
 */
@Component({
  selector: 'app-kyc-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, PageShellComponent, PaginationComponent, TranslatePipe],
  templateUrl: './kyc-dashboard.component.html',
  styleUrl: './kyc-dashboard.component.css',
})
export class KycDashboardComponent implements OnInit {
  readonly filtre = signal<KycFiltre>('pending');
  readonly dossiers = signal<KycDossier[]>([]);
  readonly compteurs = signal({ pending: 0, n1: 0, n2: 0 });
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly dossierEnCours = signal<string | null>(null);
  readonly detail = signal<KycDetail | null>(null);
  readonly detailLoading = signal(false);
  readonly page = signal(1);
  readonly taillePage = TAILLE_PAGE;
  readonly libelleTypeActeur = libelleTypeActeur;

  readonly dossiersAffiches = computed(() => paginer(this.dossiers(), this.page(), this.taillePage));
  readonly nombreEnAttente = computed(() => this.compteurs().pending);

  constructor(
    private readonly kycService: KycService,
    private readonly confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.refresh();
  }

  changerFiltre(filtre: KycFiltre): void {
    this.filtre.set(filtre);
    this.page.set(1);
    this.detail.set(null);
    this.chargerListe();
  }

  refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      pending: this.kycService.listPending(),
      n1: this.kycService.listByNiveau('NIVEAU_1'),
      n2: this.kycService.listByNiveau('NIVEAU_2'),
    }).subscribe({
      next: ({ pending, n1, n2 }) => {
        this.compteurs.set({ pending: pending.length, n1: n1.length, n2: n2.length });
        this.appliquerListeSelonFiltre(pending, n1, n2);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger la file de vérification KYC.');
        this.loading.set(false);
      },
    });
  }

  voirDetail(dossier: KycDossier): void {
    this.detailLoading.set(true);
    this.detail.set(null);
    this.kycService.detail(dossier.id).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.detailLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(`Impossible de charger le détail de ${dossier.acteurNom}.`);
        this.detailLoading.set(false);
      },
    });
  }

  fermerDetail(): void {
    this.detail.set(null);
  }

  decide(dossier: KycDossier, decision: 'VALIDE' | 'REJETE'): void {
    const label = decision === 'VALIDE' ? 'valider' : 'rejeter';
    const confirme = this.confirmationService.confirmer(
      `Confirmer : ${label} le dossier KYC de ${dossier.acteurNom} ?`
    );
    if (!confirme) {
      return;
    }

    this.dossierEnCours.set(dossier.id);
    this.kycService.decide(dossier.id, decision).subscribe({
      next: () => {
        this.dossierEnCours.set(null);
        if (this.detail()?.id === dossier.id) {
          this.detail.set(null);
        }
        this.refresh();
      },
      error: () => {
        this.errorMessage.set(`La décision sur le dossier de ${dossier.acteurNom} a échoué.`);
        this.dossierEnCours.set(null);
      },
    });
  }

  nomAfficheDetail(detail: KycDetail): string {
    if (detail.raisonSociale) {
      return detail.raisonSociale;
    }
    return `${detail.nom ?? ''} ${detail.prenom ?? ''}`.trim() || detail.telephone;
  }

  private chargerListe(): void {
    this.loading.set(true);
    const filtre = this.filtre();
    const req =
      filtre === 'pending'
        ? this.kycService.listPending()
        : this.kycService.listByNiveau(filtre);

    req.subscribe({
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

  private appliquerListeSelonFiltre(
    pending: KycDossier[],
    n1: KycDossier[],
    n2: KycDossier[]
  ): void {
    const filtre = this.filtre();
    if (filtre === 'pending') {
      this.dossiers.set(pending);
    } else if (filtre === 'NIVEAU_1') {
      this.dossiers.set(n1);
    } else {
      this.dossiers.set(n2);
    }
  }
}
