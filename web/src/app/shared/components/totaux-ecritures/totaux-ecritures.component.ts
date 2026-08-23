import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TotauxEcritures } from '../../utils/ecritures-totaux';

/** Bloc de totaux réutilisable sur les 3 rapports financiers (Admin, Bureau, Transporteur). */
@Component({
  selector: 'app-totaux-ecritures',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './totaux-ecritures.component.html',
})
export class TotauxEcrituresComponent {
  @Input({ required: true }) totaux!: TotauxEcritures;
}
