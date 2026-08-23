import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { TransporteurMissionsService } from './transporteur-missions.service';
import { Mission } from '../../../shared/models/mission.models';
import { MissionChronologieComponent } from '../../../shared/components/mission-chronologie/mission-chronologie.component';

/** FE-TRP (Sprint 7) : un Transporteur voit la chronologie de ses propres missions. */
@Component({
  selector: 'app-transporteur-missions',
  standalone: true,
  imports: [CommonModule, PageShellComponent, MissionChronologieComponent],
  templateUrl: './transporteur-missions.component.html',
})
export class TransporteurMissionsComponent implements OnInit {
  readonly missions = signal<Mission[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly missionsService: TransporteurMissionsService) {}

  ngOnInit(): void {
    this.missionsService.list().subscribe({
      next: (missions) => {
        this.missions.set(missions);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger vos missions.');
        this.loading.set(false);
      },
    });
  }
}
