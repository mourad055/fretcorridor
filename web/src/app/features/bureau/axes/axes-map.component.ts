import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AxeService } from './axe.service';
import { Axe, layoutHubs, layoutSegments } from './axe.models';
import { MissionsListComponent } from '../missions/missions-list.component';
import { PositionsListComponent } from '../positions/positions-list.component';
import { BureauChronologieComponent } from '../chronologie/bureau-chronologie.component';

/**
 * FE-BUR-01 (Sprint 3) : un Bureau voit une carte des axes de son tenant.
 * Représentation schématique (positions calculées, pas de coordonnées
 * géographiques réelles) — voir docs/adr/0007. La cartographie géospatiale
 * réelle suivra service-geo (Moteur).
 */
@Component({
  selector: 'app-axes-map',
  standalone: true,
  imports: [CommonModule, MissionsListComponent, PositionsListComponent, BureauChronologieComponent],
  templateUrl: './axes-map.component.html',
})
export class AxesMapComponent implements OnInit {
  readonly axes = signal<Axe[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly hubs = computed(() => layoutHubs(this.axes()));
  readonly segments = computed(() => layoutSegments(this.axes(), this.hubs()));

  constructor(private readonly axeService: AxeService) {}

  ngOnInit(): void {
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

  etatClass(axe: Axe): string {
    return 'axe-' + axe.etatActivation.toLowerCase();
  }
}
