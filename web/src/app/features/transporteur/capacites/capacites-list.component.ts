import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CapaciteService } from './capacite.service';
import { Capacite } from './capacite.models';
import { TransporteurMissionsComponent } from '../missions/transporteur-missions.component';

/** FE-TRP-01 (Sprint 4) : un Transporteur voit ses capacités déclarées, lecture seule. */
@Component({
  selector: 'app-capacites-list',
  standalone: true,
  imports: [CommonModule, TransporteurMissionsComponent],
  templateUrl: './capacites-list.component.html',
})
export class CapacitesListComponent implements OnInit {
  readonly capacites = signal<Capacite[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

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
