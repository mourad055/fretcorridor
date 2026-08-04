import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Mission } from '../../models/mission.models';

/** Présentation pure, réutilisée par la vue Bureau (territoire) et la vue Transporteur (ses missions). */
@Component({
  selector: 'app-mission-chronologie',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mission-chronologie.component.html',
})
export class MissionChronologieComponent {
  @Input({ required: true }) missions: Mission[] = [];
}
