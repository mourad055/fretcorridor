import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeclarationEspeces } from '../../models/declaration-especes.models';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';

/** EF-PAY-07 (S) : missions payées en espèces — mode dégradé, absence de protection signalée explicitement. */
@Component({
  selector: 'app-especes-table',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './especes-table.component.html',
})
export class EspecesTableComponent {
  @Input({ required: true }) paiements: DeclarationEspeces[] = [];
}
