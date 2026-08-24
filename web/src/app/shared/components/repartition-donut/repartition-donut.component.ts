import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface SegmentRepartition {
  label: string;
  valeur: number;
  couleur: string;
}

interface SegmentCalcule {
  segment: SegmentRepartition;
  dasharray: string;
  dashoffset: number;
  pourcentage: number;
}

/**
 * Premier widget de dataviz du produit (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.3) : SVG maison plutôt
 * qu'une librairie (ngx-charts envisagé, écarté pour ce premier widget —
 * cohérent avec la sobriété du design system, évite d'alourdir le bundle
 * pour un seul type de graphique). Respecte la « Règle de la Couleur Non
 * Seule » de DESIGN.md : la légende texte (libellé + valeur + %) est
 * toujours affichée à côté du disque, jamais la couleur seule.
 */
@Component({
  selector: 'app-repartition-donut',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './repartition-donut.component.html',
  styleUrl: './repartition-donut.component.css',
})
export class RepartitionDonutComponent {
  @Input({ required: true }) segments: SegmentRepartition[] = [];

  readonly rayon = 40;
  readonly circonference = 2 * Math.PI * this.rayon;

  get total(): number {
    return this.segments.reduce((somme, s) => somme + s.valeur, 0);
  }

  libelleAccessible(): string {
    return 'Répartition : ' + this.segments.map((s) => `${s.label} ${s.valeur}`).join(', ');
  }

  segmentsCalcules(): SegmentCalcule[] {
    const total = this.total;
    let cumulatif = 0;
    return this.segments
      .filter((s) => s.valeur > 0)
      .map((segment) => {
        const part = total > 0 ? segment.valeur / total : 0;
        const longueur = part * this.circonference;
        const dashoffset = -cumulatif;
        cumulatif += longueur;
        return {
          segment,
          dasharray: `${longueur} ${this.circonference - longueur}`,
          dashoffset,
          pourcentage: Math.round(part * 100),
        };
      });
  }
}
