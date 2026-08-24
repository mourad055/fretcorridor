import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { MissionService } from './mission.service';
import { MissionAppariee, MissionsFiltre, StatutMission } from './mission.models';
import {
  StatusBadgeComponent,
  missionStatusVariant,
  libelleMissionStatut,
} from '../../../shared/components/status-badge/status-badge.component';

/**
 * FE-BUR-01 (Sprint 5) : un Bureau voit les missions appariées de son territoire.
 * EF-BUR-02 (Sprint 14) : filtrage par statut/axe, détail d'une mission, export CSV du flux supervisé.
 */
@Component({
  selector: 'app-missions-list',
  standalone: true,
  imports: [CommonModule, RouterLink, PageShellComponent, FormsModule, StatusBadgeComponent, TranslatePipe],
  templateUrl: './missions-list.component.html',
})
export class MissionsListComponent implements OnInit {
  readonly missions = signal<MissionAppariee[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly missionStatusVariant = missionStatusVariant;
  readonly libelleMissionStatut = libelleMissionStatut;

  readonly statutFiltre = signal<StatutMission | ''>('');
  readonly axeIdFiltre = signal('');
  readonly missionSelectionnee = signal<MissionAppariee | null>(null);
  readonly detailEnCours = signal<string | null>(null);
  readonly exportEnCours = signal(false);

  constructor(private readonly missionService: MissionService) {}

  ngOnInit(): void {
    this.charger();
  }

  filtrer(): void {
    this.charger();
  }

  reinitialiserFiltres(): void {
    this.statutFiltre.set('');
    this.axeIdFiltre.set('');
    this.charger();
  }

  voirDetail(missionId: string): void {
    this.detailEnCours.set(missionId);
    this.missionService.detail(missionId).subscribe({
      next: (mission) => {
        this.missionSelectionnee.set(mission);
        this.detailEnCours.set(null);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le détail de cette mission.');
        this.detailEnCours.set(null);
      },
    });
  }

  fermerDetail(): void {
    this.missionSelectionnee.set(null);
  }

  exporter(): void {
    this.exportEnCours.set(true);
    this.missionService.exporterCsv(this.filtreActif()).subscribe({
      next: (csv) => {
        this.exportEnCours.set(false);
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = 'missions-supervisees.csv';
        lien.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.exportEnCours.set(false);
        this.errorMessage.set('Impossible d\'exporter les missions supervisées.');
      },
    });
  }

  private charger(): void {
    this.loading.set(true);
    this.missionService.list(this.filtreActif()).subscribe({
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

  private filtreActif(): MissionsFiltre {
    return {
      statut: this.statutFiltre() || undefined,
      axeId: this.axeIdFiltre() || undefined,
    };
  }
}
