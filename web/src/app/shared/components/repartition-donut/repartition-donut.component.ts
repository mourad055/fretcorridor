import { Component, Input, OnChanges } from '@angular/core';
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

@Component({
  selector: 'app-repartition-donut',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './repartition-donut.component.html',
  styleUrl: './repartition-donut.component.css',
})
export class RepartitionDonutComponent implements OnChanges {
  @Input({ required: true }) segments: SegmentRepartition[] = [];

  readonly rayon = 40;
  readonly circonference = 2 * Math.PI * this.rayon;
  total = 0;
  segmentsCalcules: SegmentCalcule[] = [];
  libelleAccessible = '';

  ngOnChanges(): void {
    this.total = this.segments.reduce((somme, s) => somme + s.valeur, 0);
    this.libelleAccessible = 'Répartition : ' + this.segments.map((s) => `${s.label} ${s.valeur}`).join(', ');
    let cumulatif = 0;
    this.segmentsCalcules = this.segments
      .filter((s) => s.valeur > 0)
      .map((segment) => {
        const part = this.total > 0 ? segment.valeur / this.total : 0;
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
