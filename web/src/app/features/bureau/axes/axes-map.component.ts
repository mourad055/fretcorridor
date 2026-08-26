import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { TranslatePipe } from '@ngx-translate/core';
import { AxeService } from './axe.service';
import { Axe } from './axe.models';
import { CorridorMapComponent } from './corridor-map.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { paginer } from '../../../shared/utils/pagination';
import { StatusBadgeComponent, axeVisibiliteVariant, axeMatchingVariant, axePaiementVariant, libelleAxeVisibilite, libelleAxeMatching, libelleAxePaiement } from '../../../shared/components/status-badge/status-badge.component';

const TAILLE_PAGE = 20;

/**
 * FE-BUR-01 (Sprint 3) : un Bureau voit une carte des axes de son tenant.
 * Carte géospatiale réelle (Leaflet), centrée sur le corridor CEMAC
 * Cameroun–Tchad — voir docs/adr/0007, addendum Sprint 12. Les coordonnées
 * de hubs proviennent d'un référentiel statique en attendant service-geo
 * (Moteur). Onglet « Axes » du tableau de bord Bureau — voir docs/adr,
 * Sprint 14 (navigation par onglets).
 */
@Component({
  selector: 'app-axes-map',
  standalone: true,
  imports: [CommonModule, PageShellComponent, CorridorMapComponent, StatusBadgeComponent, TranslatePipe, PaginationComponent],
  templateUrl: './axes-map.component.html',
  styleUrl: './axes-map.component.css',
})
export class AxesMapComponent implements OnInit {
  readonly axes = signal<Axe[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly axeSelectionneId = signal<string | null>(null);

  readonly page = signal(1);
  readonly taillePage = TAILLE_PAGE;
  readonly axesAffiches = computed(() => paginer(this.axes(), this.page(), this.taillePage));

  selectionnerAxe(axeId: string): void {
    this.axeSelectionneId.update((actuel) => (actuel === axeId ? null : axeId));
  }

  constructor(private readonly axeService: AxeService) {}

  ngOnInit(): void {
    this.page.set(1);
    this.axeService.list().subscribe({
      next: (axes) => {
        this.axes.set(axes);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les axes de votre territoire.');
        this.loading.set(false);
      },
    });
  }

  readonly axeVisibiliteVariant = axeVisibiliteVariant;
  readonly axeMatchingVariant = axeMatchingVariant;
  readonly axePaiementVariant = axePaiementVariant;
  readonly libelleAxeVisibilite = libelleAxeVisibilite;
  readonly libelleAxeMatching = libelleAxeMatching;
  readonly libelleAxePaiement = libelleAxePaiement;
}
