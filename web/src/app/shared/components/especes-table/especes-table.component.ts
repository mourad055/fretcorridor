import { Component, Input, OnChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeclarationEspeces } from '../../models/declaration-especes.models';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { PaginationComponent } from '../pagination/pagination.component';
import { paginer } from '../../utils/pagination';

const TAILLE_PAGE = 20;

/** EF-PAY-07 (S) : missions payées en espèces — mode dégradé, absence de protection signalée explicitement. */
@Component({
  selector: 'app-especes-table',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent, PaginationComponent],
  templateUrl: './especes-table.component.html',
})
export class EspecesTableComponent implements OnChanges {
  @Input({ required: true }) paiements: DeclarationEspeces[] = [];

  readonly page = signal(1);
  readonly taillePage = TAILLE_PAGE;

  ngOnChanges(): void {
    this.page.set(1);
  }

  get paiementsAffiches(): DeclarationEspeces[] {
    return paginer(this.paiements, this.page(), this.taillePage);
  }
}
