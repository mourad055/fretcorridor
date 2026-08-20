import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { CapaciteService } from './capacite.service';
import { Capacite } from './capacite.models';
import {
  StatusBadgeComponent,
  capaciteStatusVariant,
  libelleModeCollecte,
  libelleCapaciteEtat,
} from '../../../shared/components/status-badge/status-badge.component';

/**
 * FE-TRP-01 (Sprint 4) : un Transporteur voit ses capacités déclarées,
 * lecture seule. Onglet « Capacités » du tableau de bord Transporteur —
 * voir docs/adr, Sprint 14 (navigation par onglets).
 */
@Component({
  selector: 'app-capacites-list',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent, TranslatePipe],
  templateUrl: './capacites-list.component.html',
})
export class CapacitesListComponent implements OnInit {
  readonly capacites = signal<Capacite[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly capaciteStatusVariant = capaciteStatusVariant;
  readonly libelleModeCollecte = libelleModeCollecte;
  readonly libelleCapaciteEtat = libelleCapaciteEtat;

  constructor(private readonly capaciteService: CapaciteService) {}

  ngOnInit(): void {
    this.capaciteService.list().subscribe({
      next: (capacites) => {
        this.capacites.set(capacites);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger vos capacités déclarées.');
        this.loading.set(false);
      },
    });
  }
}
