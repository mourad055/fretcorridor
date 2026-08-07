import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Mission } from '../../models/mission.models';
import { libelleEtapeEtat } from '../status-badge/status-badge.component';

/** Présentation pure, réutilisée par la vue Bureau (territoire) et la vue Transporteur (ses missions). */
@Component({
  selector: 'app-mission-chronologie',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mission-chronologie.component.html',
  styleUrl: './mission-chronologie.component.css',
})
export class MissionChronologieComponent {
  @Input({ required: true }) missions: Mission[] = [];

  readonly libelleEtapeEtat = libelleEtapeEtat;
}
