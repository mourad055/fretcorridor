import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { AxeService } from '../axes/axe.service';
import { Axe } from '../axes/axe.models';
import { ObservatoireService } from './observatoire.service';
import { AlerteSeuil, Comparateur, EtatAlerte, Indicateur, ObservatoireAxe } from './observatoire.models';
import {
  libelleComparateur,
  libelleIndicateurObservatoire,
} from '../../../shared/components/status-badge/status-badge.component';

const INDICATEURS: Indicateur[] = ['NOMBRE_MISSIONS', 'PRIX_MEDIANE', 'TAUX_DESEQUILIBRE_DIRECTIONNEL'];
const COMPARATEURS: Comparateur[] = ['SUPERIEUR', 'INFERIEUR'];

/**
 * EF-BUR-03/04/05/07 : observatoire de marché par axe + alertes de seuil
 * (Sprint 5, backend déjà réel côté gateway/service-bur). Premier écran de
 * cette famille — pas de graphique ici (Phase 2 de la roadmap, une fois une
 * librairie de dataviz choisie), les indicateurs restent des chiffres bruts
 * comme le reste du design system actuel (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.3/1.4).
 */
@Component({
  selector: 'app-observatoire',
  standalone: true,
  imports: [CommonModule, FormsModule, PageShellComponent],
  templateUrl: './observatoire.component.html',
})
export class ObservatoireComponent implements OnInit {
  readonly axes = signal<Axe[]>([]);
  readonly axeSelectionne = signal<string | null>(null);
  readonly observatoire = signal<ObservatoireAxe | null>(null);
  readonly alertes = signal<EtatAlerte[]>([]);
  readonly loadingObservatoire = signal(false);
  readonly loadingAlertes = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly indicateurs = INDICATEURS;
  readonly comparateurs = COMPARATEURS;
  readonly libelleIndicateurObservatoire = libelleIndicateurObservatoire;
  readonly libelleComparateur = libelleComparateur;

  readonly volumeMensuelEstime = signal<number | null>(null);
  readonly sourceEstimation = signal('');

  readonly nouvelleAlerteAxeId = signal<string | null>(null);
  readonly nouvelleAlerteIndicateur = signal<Indicateur>('NOMBRE_MISSIONS');
  readonly nouvelleAlerteComparateur = signal<Comparateur>('SUPERIEUR');
  readonly nouvelleAlerteSeuil = signal<number | null>(null);

  constructor(
    private readonly axeService: AxeService,
    private readonly observatoireService: ObservatoireService
  ) {}

  ngOnInit(): void {
    this.axeService.list().subscribe({
      next: (axes) => {
        this.axes.set(axes);
        const premierAxe = axes[0]?.id ?? null;
        this.axeSelectionne.set(premierAxe);
        this.nouvelleAlerteAxeId.set(premierAxe);
        if (premierAxe) {
          this.chargerObservatoire(premierAxe);
        }
      },
      error: () => this.errorMessage.set('Impossible de charger la liste des axes.'),
    });
    this.chargerAlertes();
  }

  onAxeChange(axeId: string): void {
    this.axeSelectionne.set(axeId);
    this.chargerObservatoire(axeId);
  }

  private chargerObservatoire(axeId: string): void {
    this.loadingObservatoire.set(true);
    this.observatoireService.observatoirePourAxe(axeId).subscribe({
      next: (vue) => {
        this.observatoire.set(vue);
        this.loadingObservatoire.set(false);
      },
      error: () => {
        this.errorMessage.set("Impossible de charger l'observatoire de cet axe.");
        this.loadingObservatoire.set(false);
      },
    });
  }

  private chargerAlertes(): void {
    this.loadingAlertes.set(true);
    this.observatoireService.etatAlertes().subscribe({
      next: (etats) => {
        this.alertes.set(etats);
        this.loadingAlertes.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les alertes de seuil.');
        this.loadingAlertes.set(false);
      },
    });
  }

  definirEstimation(): void {
    const axeId = this.axeSelectionne();
    const volume = this.volumeMensuelEstime();
    const source = this.sourceEstimation().trim();
    if (!axeId || volume === null || !source) {
      return;
    }
    this.observatoireService.definirEstimationMarche(axeId, volume, source).subscribe({
      next: () => {
        this.sourceEstimation.set('');
        this.volumeMensuelEstime.set(null);
        this.chargerObservatoire(axeId);
      },
      error: () => this.errorMessage.set("Impossible d'enregistrer l'estimation de marché."),
    });
  }

  configurerAlerte(): void {
    const axeId = this.nouvelleAlerteAxeId();
    const seuil = this.nouvelleAlerteSeuil();
    if (!axeId || seuil === null) {
      return;
    }
    this.observatoireService
      .configurerAlerte(axeId, this.nouvelleAlerteIndicateur(), this.nouvelleAlerteComparateur(), seuil)
      .subscribe({
        next: () => {
          this.nouvelleAlerteSeuil.set(null);
          this.chargerAlertes();
        },
        error: () => this.errorMessage.set("Impossible de configurer l'alerte."),
      });
  }

  supprimerAlerte(alerte: AlerteSeuil): void {
    this.observatoireService.supprimerAlerte(alerte.id).subscribe({
      next: () => this.chargerAlertes(),
      error: () => this.errorMessage.set("Impossible de supprimer l'alerte."),
    });
  }

  libelleAxe(axeId: string): string {
    const axe = this.axes().find((a) => a.id === axeId);
    return axe ? `${axe.origine} → ${axe.destination}` : axeId;
  }
}
