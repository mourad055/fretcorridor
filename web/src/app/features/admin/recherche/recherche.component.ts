import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { RechercheService } from './recherche.service';
import { ResultatRecherche } from './recherche.models';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { paginer } from '../../../shared/utils/pagination';

const TAILLE_PAGE = 20;

/**
 * Recherche globale transverse (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.5).
 */
@Component({
  selector: 'app-recherche',
  standalone: true,
  imports: [CommonModule, FormsModule, PageShellComponent, PaginationComponent],
  templateUrl: './recherche.component.html',
})
export class RechercheComponent {
  readonly terme = signal('');
  readonly resultats = signal<ResultatRecherche[] | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly page = signal(1);
  readonly taillePage = TAILLE_PAGE;
  readonly resultatsAffiches = computed(() => paginer(this.resultats() ?? [], this.page(), this.taillePage));

  constructor(private readonly rechercheService: RechercheService) {}

  rechercher(): void {
    const terme = this.terme().trim();
    if (!terme) {
      this.resultats.set(null);
      this.page.set(1);
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);
    this.page.set(1);
    this.rechercheService.rechercher(terme).subscribe({
      next: (resultats) => {
        this.resultats.set(resultats);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de lancer la recherche.');
        this.loading.set(false);
      },
    });
  }

  libelleType(type: ResultatRecherche['type']): string {
    return type === 'TENANT' ? 'Tenant' : "Journal d'audit";
  }
}
