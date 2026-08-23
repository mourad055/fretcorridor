import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { BureauChronologieService } from './bureau-chronologie.service';
import { Mission } from '../../../shared/models/mission.models';
import { MissionChronologieComponent } from '../../../shared/components/mission-chronologie/mission-chronologie.component';

/** FE-BUR (Sprint 7) : un Bureau supervise la chronologie des missions de son territoire. */
@Component({
  selector: 'app-bureau-chronologie',
  standalone: true,
  imports: [CommonModule, PageShellComponent, MissionChronologieComponent],
  templateUrl: './bureau-chronologie.component.html',
})
export class BureauChronologieComponent implements OnInit {
  readonly missions = signal<Mission[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly chronologieService: BureauChronologieService) {}

  ngOnInit(): void {
    this.chronologieService.list().subscribe({
      next: (missions) => {
        this.missions.set(missions);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger la chronologie des missions.');
        this.loading.set(false);
      },
    });
  }
}
