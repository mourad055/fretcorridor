import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ecriture } from '../../models/ecriture.models';

@Component({
  selector: 'app-ecritures-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ecritures-table.component.html',
})
export class EcrituresTableComponent {
  @Input({ required: true }) ecritures: Ecriture[] = [];
}
