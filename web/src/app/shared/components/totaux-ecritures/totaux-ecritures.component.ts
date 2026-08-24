import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TotauxEcritures } from '../../utils/ecritures-totaux';
import { RepartitionDonutComponent, SegmentRepartition } from '../repartition-donut/repartition-donut.component';

/**
 * Bloc de totaux réutilisable sur les 3 rapports financiers (Admin, Bureau,
 * Transporteur). Donut crédit/débit ajouté (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.3) — premier graphique
 * du produit, diffusé aux 3 rôles d'un coup puisqu'ils partagent déjà ce
 * composant.
 */
@Component({
  selector: 'app-totaux-ecritures',
  standalone: true,
  imports: [CommonModule, RepartitionDonutComponent],
  templateUrl: './totaux-ecritures.component.html',
  styleUrl: './totaux-ecritures.component.css',
})
export class TotauxEcrituresComponent {
  @Input({ required: true }) totaux!: TotauxEcritures;

  // Hex brut plutôt que var(--fc-success/--fc-danger) : même choix que
  // CorridorMapComponent (couleurPourAxe) pour les couleurs SVG — cohérence
  // de style dans le dépôt plutôt qu'une dépendance aux custom properties
  // dans un attribut de présentation SVG.
  get segmentsRepartition(): SegmentRepartition[] {
    return [
      { label: 'Crédité', valeur: this.totaux.totalCredit, couleur: '#067647' },
      { label: 'Débité', valeur: this.totaux.totalDebit, couleur: '#b42318' },
    ];
  }
}
