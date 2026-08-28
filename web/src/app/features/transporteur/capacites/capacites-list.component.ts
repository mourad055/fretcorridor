import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { TranslatePipe } from '@ngx-translate/core';
import { CapaciteService } from './capacite.service';
import { Capacite } from './capacite.models';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { paginer } from '../../../shared/utils/pagination';
import {
  StatusBadgeComponent,
  capaciteStatusVariant,
  libelleModeCollecte,
  libelleCapaciteEtat,
} from '../../../shared/components/status-badge/status-badge.component';

const TAILLE_PAGE = 20;

/**
 * FE-TRP-01 (Sprint 4) : un Transporteur voit ses capacités déclarées,
 * lecture seule. Onglet « Capacités » du tableau de bord Transporteur —
 * voir docs/adr, Sprint 14 (navigation par onglets).
 */
@Component({
  selector: 'app-capacites-list',
  standalone: true,
  imports: [CommonModule, PageShellComponent, StatusBadgeComponent, TranslatePipe, PaginationComponent],
  templateUrl: './capacites-list.component.html',
})
export class CapacitesListComponent implements OnInit {
  readonly capacites = signal<Capacite[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly capaciteStatusVariant = capaciteStatusVariant;
  readonly libelleModeCollecte = libelleModeCollecte;
  readonly libelleCapaciteEtat = libelleCapaciteEtat;

  readonly page = signal(1);
  readonly taillePage = TAILLE_PAGE;
  readonly capacitesAffiches = computed(() => paginer(this.capacites(), this.page(), this.taillePage));

  constructor(private readonly capaciteService: CapaciteService) {}

  ngOnInit(): void {
    this.page.set(1);
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
