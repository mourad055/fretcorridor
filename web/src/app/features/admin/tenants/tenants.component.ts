import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { FormsModule } from '@angular/forms';
import { TenantsService } from './tenants.service';
import { Tenant } from '../../../shared/models/tenant.models';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { paginer } from '../../../shared/utils/pagination';
import { ConfirmationService } from '../../../shared/services/confirmation.service';

const TAILLE_PAGE = 20;

/**
 * FE-ADM-04 (Sprint 10) : gestion des tenants.
 *
 * Édition, statut actif/inactif et recherche ajoutés (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.2) : jusqu'ici le
 * module était create-only, sans aucun moyen de corriger un nom/pays ou de
 * désactiver un tenant obsolète.
 */
@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [CommonModule, PageShellComponent, FormsModule, PaginationComponent],
  templateUrl: './tenants.component.html',
})
export class TenantsComponent implements OnInit {
  readonly tenants = signal<Tenant[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly recherche = signal('');

  /** KPI en tête d'écran (audit UX 2026-08-23, §1.6). */
  readonly nombreActifs = computed(() => this.tenants().filter((t) => t.actif).length);

  readonly tenantsFiltres = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    if (!terme) {
      return this.tenants();
    }
    return this.tenants().filter(
      (t) => t.id.toLowerCase().includes(terme) || t.nom.toLowerCase().includes(terme) || t.pays.toLowerCase().includes(terme)
    );
  });

  // Pagination (audit UX 2026-08-23, §3.3).
  readonly page = signal(1);
  readonly taillePage = TAILLE_PAGE;
  readonly tenantsAffiches = computed(() => paginer(this.tenantsFiltres(), this.page(), this.taillePage));

  readonly nouvelId = signal('');
  readonly nouvelNom = signal('');
  readonly nouveauPays = signal('');

  readonly tenantEnEdition = signal<string | null>(null);
  readonly editNom = signal('');
  readonly editPays = signal('');
  readonly editActif = signal(true);

  constructor(
    private readonly tenantsService: TenantsService,
    private readonly confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  onRechercheChange(terme: string): void {
    this.recherche.set(terme);
    this.page.set(1);
  }

  charger(): void {
    this.loading.set(true);
    this.page.set(1);
    this.tenantsService.lister().subscribe({
      next: (tenants) => {
        this.tenants.set(tenants);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les tenants.');
        this.loading.set(false);
      },
    });
  }

  creer(): void {
    const confirme = this.confirmationService.confirmer(
      `Créer le tenant « ${this.nouvelNom()} » (${this.nouvelId()}) ?`
    );
    if (!confirme) {
      return;
    }
    this.tenantsService.creer(this.nouvelId(), this.nouvelNom(), this.nouveauPays()).subscribe({
      next: () => {
        this.nouvelId.set('');
        this.nouvelNom.set('');
        this.nouveauPays.set('');
        this.charger();
      },
      error: () => this.errorMessage.set('Impossible de créer ce tenant.'),
    });
  }

  commencerEdition(tenant: Tenant): void {
    this.tenantEnEdition.set(tenant.id);
    this.editNom.set(tenant.nom);
    this.editPays.set(tenant.pays);
    this.editActif.set(tenant.actif);
  }

  annulerEdition(): void {
    this.tenantEnEdition.set(null);
  }

  enregistrerEdition(): void {
    const id = this.tenantEnEdition();
    if (!id) {
      return;
    }
    this.tenantsService.modifier(id, this.editNom(), this.editPays(), this.editActif()).subscribe({
      next: () => {
        this.tenantEnEdition.set(null);
        this.charger();
      },
      error: () => this.errorMessage.set('Impossible de modifier ce tenant.'),
    });
  }
}
