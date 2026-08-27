import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TotauxEcritures } from '../../utils/ecritures-totaux';
import { RepartitionDonutComponent, SegmentRepartition } from '../repartition-donut/repartition-donut.component';

@Component({
  selector: 'app-totaux-ecritures',
  standalone: true,
  imports: [CommonModule, RepartitionDonutComponent],
  templateUrl: './totaux-ecritures.component.html',
  styleUrl: './totaux-ecritures.component.css',
})
export class TotauxEcrituresComponent implements OnChanges {
  @Input({ required: true }) totaux!: TotauxEcritures;
  @Input() variante: 'bureau' | 'transporteur' = 'bureau';

  segmentsRepartition: SegmentRepartition[] = [];

  ngOnChanges(): void {
    if (!this.totaux) {
      this.segmentsRepartition = [];
      return;
    }
    if (this.variante === 'transporteur') {
      this.segmentsRepartition = [{ label: 'Reçu', valeur: this.totaux.solde, couleur: '#067647' }];
      return;
    }
    this.segmentsRepartition = [
      { label: 'Crédité', valeur: this.totaux.totalCredit, couleur: '#067647' },
      { label: 'Débité', valeur: this.totaux.totalDebit, couleur: '#b42318' },
    ];
  }
}
