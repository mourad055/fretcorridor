import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { FormsModule } from '@angular/forms';
import { JournalAuditService } from './journal-audit.service';
import { TenantsService } from '../tenants/tenants.service';
import { EntreeJournalAudit } from '../../../shared/models/journal-audit.models';
import { Tenant } from '../../../shared/models/tenant.models';
import { libelleJournalAction } from '../../../shared/components/status-badge/status-badge.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { paginer } from '../../../shared/utils/pagination';

const TAILLE_PAGE = 20;

/** Valeur du sélecteur de tenant représentant une consultation transverse à tous les tenants. */
const TOUS_LES_TENANTS = '';

/**
 * FE-ADM-05 (Sprint 10) : journal d'audit consultable et exportable, en lecture seule, append-only.
 *
 * Filtre de tenant rendu explicite côté UI (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §3.1/0.3) : le paramètre
 * tenantId existait déjà côté service mais n'était jamais exposé à l'écran —
 * un ADMINISTRATION consultait/exportait silencieusement tous les tenants
 * sans jamais faire ce choix explicitement. "Tous les tenants" reste une
 * option légitime (consultation transverse ADMINISTRATION, cf.
 * JournalAuditController#tenantIdAutorise côté service-adm), mais c'est
 * désormais une sélection visible, jamais un défaut silencieux.
 */
@Component({
  selector: 'app-journal-audit',
  standalone: true,
  imports: [CommonModule, PageShellComponent, FormsModule, PaginationComponent],
  templateUrl: './journal-audit.component.html',
  styleUrl: './journal-audit.component.css',
})
export class JournalAuditComponent implements OnInit {
  readonly entrees = signal<EntreeJournalAudit[]>([]);
  readonly tenants = signal<Tenant[]>([]);
  readonly tenantIdSelectionne = signal<string>(TOUS_LES_TENANTS);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly libelleJournalAction = libelleJournalAction;
  readonly tousLesTenants = TOUS_LES_TENANTS;

  // Pagination (audit UX 2026-08-23, §3.3) : journal append-only, le plus
  // susceptible de tous les écrans de grossir sans limite.
  readonly page = signal(1);
  readonly taillePage = TAILLE_PAGE;
  readonly entreesAffichees = computed(() => paginer(this.entrees(), this.page(), this.taillePage));

  constructor(
    private readonly journalAuditService: JournalAuditService,
    private readonly tenantsService: TenantsService
  ) {}

  ngOnInit(): void {
    this.tenantsService.lister().subscribe({
      next: (tenants) => this.tenants.set(tenants),
      error: () => undefined, // la liste de tenants n'est qu'un confort de filtre, pas bloquant pour consulter le journal
    });
    this.charger();
  }

  onTenantChange(tenantId: string): void {
    this.tenantIdSelectionne.set(tenantId);
    this.charger();
  }

  private charger(): void {
    this.loading.set(true);
    this.page.set(1);
    const tenantId = this.tenantIdSelectionne() || undefined;
    this.journalAuditService.lister(tenantId).subscribe({
      next: (entrees) => {
        this.entrees.set(entrees);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger le journal d\'audit.');
        this.loading.set(false);
      },
    });
  }

  exporter(): void {
    const tenantId = this.tenantIdSelectionne() || undefined;
    this.journalAuditService.exporterCsv(tenantId).subscribe({
      next: (csv) => {
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = 'journal-audit.csv';
        lien.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.errorMessage.set('Impossible d\'exporter le journal d\'audit.'),
    });
  }
}
