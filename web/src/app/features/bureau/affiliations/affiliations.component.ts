import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { ConfirmationService } from '../../../shared/services/confirmation.service';
import { AffiliationService } from './affiliation.service';

/** Motifs de refus renvoyés par service-ida (AffiliationService#inviter) — vocabulaire fermé, jamais affiché littéralement. */
function cleErreurInvitation(erreur: HttpErrorResponse): string {
  const motif = erreur.error?.detail as string | undefined;
  if (motif === 'ACTEUR_INTROUVABLE') {
    return 'affiliations.erreur.acteurIntrouvable';
  }
  if (motif === 'ROLE_NON_AFFILIABLE') {
    return 'affiliations.erreur.roleNonAffiliable';
  }
  if (erreur.status === 503) {
    return 'affiliations.erreur.indisponible';
  }
  return 'affiliations.erreur.generique';
}

/**
 * S18 : un opérateur du second bureau rattache un transporteur/chauffeur
 * existant à son tenant en l'invitant par téléphone — l'invitation EST la
 * validation (règle produit), aucun flux d'acceptation côté transporteur.
 * Idempotent côté backend : réinviter un acteur déjà affilié ne duplique rien.
 */
@Component({
  selector: 'app-affiliations',
  standalone: true,
  imports: [CommonModule, PageShellComponent, FormsModule, TranslatePipe],
  templateUrl: './affiliations.component.html',
})
export class AffiliationsComponent {
  readonly telephone = signal('');
  readonly enCours = signal(false);
  readonly succesTelephone = signal<string | null>(null);
  readonly errorMessageKey = signal<string | null>(null);

  constructor(
    private readonly affiliationService: AffiliationService,
    private readonly confirmationService: ConfirmationService,
    private readonly translate: TranslateService
  ) {}

  inviter(): void {
    const telephone = this.telephone().trim();
    if (!telephone || this.enCours()) {
      return;
    }
    const confirme = this.confirmationService.confirmer(
      this.translate.instant('affiliations.confirmation', { telephone })
    );
    if (!confirme) {
      return;
    }
    this.enCours.set(true);
    this.errorMessageKey.set(null);
    this.succesTelephone.set(null);
    this.affiliationService.inviter(telephone).subscribe({
      next: () => {
        this.enCours.set(false);
        this.succesTelephone.set(telephone);
        this.telephone.set('');
      },
      error: (erreur: HttpErrorResponse) => {
        this.enCours.set(false);
        this.errorMessageKey.set(cleErreurInvitation(erreur));
      },
    });
  }
}
