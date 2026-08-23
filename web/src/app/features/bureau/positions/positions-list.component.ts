import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { PositionService } from './position.service';
import { Position, formatAge } from './position.models';
import { PositionsMapComponent } from './positions-map.component';

/**
 * FE-TRK-04 / RG-043 (Sprint 6) : un Bureau voit le suivi temps réel de son
 * territoire — chaque position affiche son âge, jamais un horodatage seul.
 *
 * Carte ajoutée (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.5) : le PRD (Sprint 6)
 * exigeait déjà explicitement une carte, jamais construite — le tableau
 * reste en vue de repli, même principe que l'écran Axes.
 */
@Component({
  selector: 'app-positions-list',
  standalone: true,
  imports: [CommonModule, PageShellComponent, PositionsMapComponent],
  templateUrl: './positions-list.component.html',
})
export class PositionsListComponent implements OnInit {
  readonly positions = signal<Position[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly positionService: PositionService) {}

  ngOnInit(): void {
    this.positionService.list().subscribe({
      next: (positions) => {
        this.positions.set(positions);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le suivi temps réel.');
        this.loading.set(false);
      },
    });
  }

  age(position: Position): string {
    return formatAge(position.ageSecondes);
  }
}
