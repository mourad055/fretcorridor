import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { RapportFinancierService } from './rapport-financier.service';
import { Ecriture } from '../../../shared/models/ecriture.models';
import { DeclarationEspeces } from '../../../shared/models/declaration-especes.models';
import { EcrituresTableComponent } from '../../../shared/components/ecritures-table/ecritures-table.component';
import { EspecesTableComponent } from '../../../shared/components/especes-table/especes-table.component';
import { TotauxEcrituresComponent } from '../../../shared/components/totaux-ecritures/totaux-ecritures.component';
import { calculerTotauxEcritures, ecrituresVersCsv, telechargerCsv } from '../../../shared/utils/ecritures-totaux';

/**
 * Rapport financier (Sprint 8, lecture seule) : un Bureau voit les écritures
 * de son territoire.
 *
 * Filtre `missionId` en query param (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.7) : drill-down
 * mission → écritures depuis l'écran Missions — les deux écrans étaient
 * jusqu'ici cloisonnés, aucune navigation croisée. Filtre purement côté
 * client (l'appel serveur reste inchangé, `rapport()` charge tout le
 * territoire).
 */
@Component({
  selector: 'app-rapport-financier',
  standalone: true,
  imports: [CommonModule, RouterLink, PageShellComponent, EcrituresTableComponent, EspecesTableComponent, TotauxEcrituresComponent],
  templateUrl: './rapport-financier.component.html',
})
export class RapportFinancierComponent implements OnInit {
  readonly ecritures = signal<Ecriture[]>([]);
  readonly paiementsEspeces = signal<DeclarationEspeces[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly missionIdFiltre = signal<string | null>(null);

  readonly ecrituresAffichees = computed(() => {
    const missionId = this.missionIdFiltre();
    return missionId ? this.ecritures().filter((e) => e.missionId === missionId) : this.ecritures();
  });
  readonly totaux = computed(() => calculerTotauxEcritures(this.ecrituresAffichees()));

  constructor(
    private readonly rapportFinancierService: RapportFinancierService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  exporter(): void {
    telechargerCsv('rapport-financier-bureau.csv', ecrituresVersCsv(this.ecrituresAffichees()));
  }

  reinitialiserFiltre(): void {
    void this.router.navigate([], { queryParams: {} });
  }

  ngOnInit(): void {
    this.missionIdFiltre.set(this.route.snapshot.queryParamMap.get('missionId'));
    this.route.queryParamMap.subscribe((params) => this.missionIdFiltre.set(params.get('missionId')));

    this.rapportFinancierService.rapport().subscribe({
      next: (ecritures) => {
        this.ecritures.set(ecritures);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le rapport financier.');
        this.loading.set(false);
      },
    });

    this.rapportFinancierService.paiementsEspeces().subscribe({
      next: (paiements) => this.paiementsEspeces.set(paiements),
      error: () => this.paiementsEspeces.set([]),
    });
  }
}
