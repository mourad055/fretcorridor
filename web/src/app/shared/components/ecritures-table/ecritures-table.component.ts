import { Component, Input, OnChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { Ecriture } from '../../models/ecriture.models';
import { StatusBadgeComponent, ecritureStatusVariant, libelleEcritureStatut, libelleModePaiement } from '../status-badge/status-badge.component';
import { PaginationComponent } from '../pagination/pagination.component';
import { paginer } from '../../utils/pagination';

const TAILLE_PAGE = 20;

/**
 * Pagination interne (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §3.3) : géré ici plutôt
 * que dans chacun des 4 écrans consommateurs (Admin/Bureau/Transporteur
 * rapports financiers + solde transporteur) — un seul changement profite
 * aux 4 à la fois. Les totaux/export du parent continuent de porter sur la
 * liste complète, seul l'affichage tabulaire est paginé.
 */
@Component({
  selector: 'app-ecritures-table',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent, TranslatePipe, PaginationComponent],
  templateUrl: './ecritures-table.component.html',
})
export class EcrituresTableComponent implements OnChanges {
  @Input({ required: true }) ecritures: Ecriture[] = [];

  readonly page = signal(1);
  readonly taillePage = TAILLE_PAGE;

  readonly ecritureStatusVariant = ecritureStatusVariant;
  readonly libelleEcritureStatut = libelleEcritureStatut;
  readonly libelleModePaiement = libelleModePaiement;

  ngOnChanges(): void {
    this.page.set(1);
  }

  get ecrituresAffichees(): Ecriture[] {
    return paginer(this.ecritures, this.page(), this.taillePage);
  }
}
