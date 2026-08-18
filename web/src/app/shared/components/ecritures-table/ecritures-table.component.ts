import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ecriture } from '../../models/ecriture.models';
import { StatusBadgeComponent, ecritureStatusVariant, libelleEcritureStatut, libelleModePaiement } from '../status-badge/status-badge.component';

@Component({
  selector: 'app-ecritures-table',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './ecritures-table.component.html',
})
export class EcrituresTableComponent {
  @Input({ required: true }) ecritures: Ecriture[] = [];

  readonly ecritureStatusVariant = ecritureStatusVariant;
  readonly libelleEcritureStatut = libelleEcritureStatut;
  readonly libelleModePaiement = libelleModePaiement;
}
