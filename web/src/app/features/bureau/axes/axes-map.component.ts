import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AxeService } from './axe.service';
import { Axe } from './axe.models';
import { CorridorMapComponent } from './corridor-map.component';
import { MissionsListComponent } from '../missions/missions-list.component';
import { PositionsListComponent } from '../positions/positions-list.component';
import { BureauChronologieComponent } from '../chronologie/bureau-chronologie.component';
import { RapportFinancierComponent } from '../rapport-financier/rapport-financier.component';
import { NotificationsComponent } from '../notifications/notifications.component';
import { StatusBadgeComponent, axeStatusVariant } from '../../../shared/components/status-badge/status-badge.component';

/**
 * FE-BUR-01 (Sprint 3) : un Bureau voit une carte des axes de son tenant.
 * Carte géospatiale réelle (Leaflet), centrée sur le corridor CEMAC
 * Cameroun–Tchad — voir docs/adr/0007, addendum Sprint 12. Les coordonnées
 * de hubs proviennent d'un référentiel statique en attendant service-geo
 * (Moteur).
 */
@Component({
  selector: 'app-axes-map',
  standalone: true,
  imports: [
    CommonModule,
    CorridorMapComponent,
    MissionsListComponent,
    PositionsListComponent,
    BureauChronologieComponent,
    RapportFinancierComponent,
    NotificationsComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './axes-map.component.html',
})
export class AxesMapComponent implements OnInit {
  readonly axes = signal<Axe[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

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

  readonly axeStatusVariant = axeStatusVariant;
}
