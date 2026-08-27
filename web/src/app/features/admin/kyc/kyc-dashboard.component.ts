import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { forkJoin } from 'rxjs';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { TranslatePipe } from '@ngx-translate/core';
import { paginer } from '../../../shared/utils/pagination';
import { KycService } from './kyc.service';
import { KycDetail, KycDossier, KycFiltre, KycPiece } from './kyc.models';
import { libelleTypeActeur } from '../../../shared/components/status-badge/status-badge.component';
import { TENANTS_ADMIN, TENANT_ADMIN_DEFAUT } from '../admin-tenants';

const TAILLE_PAGE = 20;

interface PieceOuverte {
  piece: KycPiece;
  urlImage: string;
  urlDocument: SafeResourceUrl;
  typeMime: string;
}

/**
 * FE-ADM-06 : file de revue KYC réelle (IDA) — filtres En attente / N1 / N2,
 * détail pièces présignées, valider/rejeter. Pas de CRUD créer/supprimer acteur
 * (lien secondaire vers /admin/comptes).
 */
@Component({
  selector: 'app-kyc-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PageShellComponent, PaginationComponent, TranslatePipe],
  templateUrl: './kyc-dashboard.component.html',
  styleUrl: './kyc-dashboard.component.css',
})
export class KycDashboardComponent implements OnInit, OnDestroy {
  readonly tenants = TENANTS_ADMIN;
  readonly tenantSelectionne = signal<string>(TENANT_ADMIN_DEFAUT);
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
  readonly confirmation = signal<{ dossier: KycDossier; decision: 'VALIDE' | 'REJETE' } | null>(null);
  readonly pieceOuverte = signal<PieceOuverte | null>(null);
  readonly pieceChargement = signal(false);

  readonly dossiersAffiches = computed(() => paginer(this.dossiers(), this.page(), this.taillePage));
  readonly nombreEnAttente = computed(() => this.compteurs().pending);

  constructor(
    private readonly kycService: KycService,
    private readonly sanitizer: DomSanitizer,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const tenantQuery = this.route.snapshot.queryParamMap.get('tenantId');
    if (tenantQuery && (this.tenants as readonly string[]).includes(tenantQuery)) {
      this.tenantSelectionne.set(tenantQuery);
    }
    this.refresh();
  }

  ngOnDestroy(): void {
    this.revoquerUrlPiece();
  }

  changerTenant(): void {
    this.page.set(1);
    this.fermerDetail();
    this.refresh();
  }

  changerFiltre(filtre: KycFiltre): void {
    this.filtre.set(filtre);
    this.page.set(1);
    this.fermerDetail();
    this.chargerListe();
  }

  refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const tenantId = this.tenantSelectionne();
    forkJoin({
      pending: this.kycService.listPending(tenantId),
      n1: this.kycService.listByNiveau(tenantId, 'NIVEAU_1'),
      n2: this.kycService.listByNiveau(tenantId, 'NIVEAU_2'),
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
    this.fermerPiece();
    this.kycService.detail(this.tenantSelectionne(), dossier.id).subscribe({
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
    this.fermerPiece();
  }

  demanderDecision(dossier: KycDossier, decision: 'VALIDE' | 'REJETE'): void {
    this.confirmation.set({ dossier, decision });
  }

  annulerDecision(): void {
    this.confirmation.set(null);
  }

  confirmerDecision(): void {
    const enCours = this.confirmation();
    if (!enCours) {
      return;
    }
    this.confirmation.set(null);
    this.executerDecision(enCours.dossier, enCours.decision);
  }

  libelleDecision(decision: 'VALIDE' | 'REJETE'): string {
    return decision === 'VALIDE' ? 'Valider' : 'Rejeter';
  }

  ouvrirPiece(piece: KycPiece): void {
    const detail = this.detail();
    if (!detail) {
      return;
    }

    this.revoquerUrlPiece();
    this.pieceChargement.set(true);
    this.pieceOuverte.set(null);

    this.kycService.chargerPiece(this.tenantSelectionne(), detail.id, piece.id).subscribe({
      next: (blob) => {
        const typeMime = this.devinerTypeMime(blob, piece);
        const typedBlob =
          blob.type === typeMime ? blob : new Blob([blob], { type: typeMime });
        const urlImage = URL.createObjectURL(typedBlob);
        this.pieceOuverte.set({
          piece,
          urlImage,
          urlDocument: this.sanitizer.bypassSecurityTrustResourceUrl(urlImage),
          typeMime,
        });
        this.pieceChargement.set(false);
      },
      error: () => {
        this.errorMessage.set(`Impossible d'afficher la pièce ${piece.typeDocument}.`);
        this.pieceChargement.set(false);
      },
    });
  }

  fermerPiece(): void {
    this.revoquerUrlPiece();
    this.pieceChargement.set(false);
  }

  pieceEstImage(typeMime: string): boolean {
    return typeMime.startsWith('image/');
  }

  pieceEstPdf(typeMime: string): boolean {
    return typeMime === 'application/pdf';
  }

  private devinerTypeMime(blob: Blob, piece: KycPiece): string {
    if (blob.type && blob.type !== 'application/octet-stream') {
      return blob.type;
    }
    if (piece.typeDocument === 'CNI' || piece.typeDocument === 'PERMIS') {
      return 'image/jpeg';
    }
    return blob.type || 'application/octet-stream';
  }

  nomAfficheDetail(detail: KycDetail): string {
    if (detail.raisonSociale) {
      return detail.raisonSociale;
    }
    return `${detail.nom ?? ''} ${detail.prenom ?? ''}`.trim() || detail.telephone;
  }

  ouvrirPieceDansNouvelOnglet(): void {
    const ouverte = this.pieceOuverte();
    if (!ouverte) {
      return;
    }
    const nouvelOnglet = window.open('', '_blank');
    if (!nouvelOnglet) {
      this.errorMessage.set('Autorisez les fenêtres pop-up pour ouvrir le document.');
      return;
    }
    nouvelOnglet.document.title = ouverte.piece.typeDocument;
    if (this.pieceEstImage(ouverte.typeMime)) {
      nouvelOnglet.document.body.style.margin = '0';
      nouvelOnglet.document.body.innerHTML =
        `<img src="${ouverte.urlImage}" alt="Document ${ouverte.piece.typeDocument}" style="max-width:100%;height:auto;display:block;margin:0 auto;">`;
      return;
    }
    nouvelOnglet.location.href = ouverte.urlImage;
  }

  telechargerPiece(): void {
    const ouverte = this.pieceOuverte();
    if (!ouverte) {
      return;
    }
    const extension = this.pieceEstPdf(ouverte.typeMime) ? 'pdf' : 'jpg';
    const lien = document.createElement('a');
    lien.href = ouverte.urlImage;
    lien.download = `${ouverte.piece.typeDocument}-${ouverte.piece.id}.${extension}`;
    lien.click();
  }

  private revoquerUrlPiece(): void {
    const ouverte = this.pieceOuverte();
    if (ouverte?.urlImage) {
      URL.revokeObjectURL(ouverte.urlImage);
    }
    this.pieceOuverte.set(null);
  }

  private executerDecision(dossier: KycDossier, decision: 'VALIDE' | 'REJETE'): void {
    this.dossierEnCours.set(dossier.id);
    this.kycService.decide(this.tenantSelectionne(), dossier.id, decision).subscribe({
      next: () => {
        this.dossierEnCours.set(null);
        if (this.detail()?.id === dossier.id) {
          this.fermerDetail();
        }
        this.refresh();
      },
      error: () => {
        this.errorMessage.set(`La décision sur le dossier de ${dossier.acteurNom} a échoué.`);
        this.dossierEnCours.set(null);
      },
    });
  }

  private chargerListe(): void {
    this.loading.set(true);
    const filtre = this.filtre();
    const tenantId = this.tenantSelectionne();
    const req =
      filtre === 'pending'
        ? this.kycService.listPending(tenantId)
        : this.kycService.listByNiveau(tenantId, filtre);

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
