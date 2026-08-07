import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MissionService } from './mission.service';
import { MissionAppariee } from './mission.models';
import {
  StatusBadgeComponent,
  missionStatusVariant,
  libelleMissionStatut,
} from '../../../shared/components/status-badge/status-badge.component';

/** FE-BUR-01 (Sprint 5) : un Bureau voit les missions appariées de son territoire. */
@Component({
  selector: 'app-missions-list',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './missions-list.component.html',
})
export class MissionsListComponent implements OnInit {
  readonly missions = signal<MissionAppariee[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly missionStatusVariant = missionStatusVariant;
  readonly libelleMissionStatut = libelleMissionStatut;

  constructor(private readonly missionService: MissionService) {}

  ngOnInit(): void {
    this.missionService.list().subscribe({
      next: (missions) => {
        this.missions.set(missions);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les missions appariées.');
        this.loading.set(false);
      },
    });
  }
}
