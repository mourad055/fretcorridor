import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { CompteService } from './compte.service';
import { CompteAdmin, ROLES_ACTEUR, RoleActeur } from './compte.models';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { paginer } from '../../../shared/utils/pagination';
import { ConfirmationService } from '../../../shared/services/confirmation.service';

const TAILLE_PAGE = 20;

/**
 * Gestion des comptes par un Admin (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.1) : c'était le trou le
 * plus net derrière le reproche "gestion clients" — jusqu'ici un Admin
 * pouvait valider un KYC ou créer un tenant, jamais gérer un compte
 * individuel (désactiver, changer de rôle).
 *
 * La création de compte et la réinitialisation du PIN restent hors
 * périmètre ici (roadmap §3) : la création passe déjà par des flux dédiés
 * (inscription mobile, enrôlement agent terrain) et un reset de PIN sûr
 * exige un flux OTP/SMS qu'un admin ne doit jamais court-circuiter.
 */
@Component({
  selector: 'app-comptes',
  standalone: true,
  imports: [CommonModule, PageShellComponent, FormsModule, PaginationComponent],
  templateUrl: './comptes.component.html',
})
export class ComptesComponent {
  readonly tenants = ['tenant-bgft-douala', 'tenant-bnft-ndjamena', 'tenant-flysoft'];
  readonly tenantSelectionne = signal(this.tenants[0]);
  readonly comptes = signal<CompteAdmin[] | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly rolesDisponibles = ROLES_ACTEUR;

  readonly compteEnEdition = signal<string | null>(null);
  readonly rolesEdition = signal<Set<RoleActeur>>(new Set());

  // Pagination (audit UX 2026-08-23, §3.3).
  readonly page = signal(1);
  readonly taillePage = TAILLE_PAGE;
  readonly comptesAffiches = computed(() => paginer(this.comptes() ?? [], this.page(), this.taillePage));

  constructor(
    private readonly compteService: CompteService,
    private readonly confirmationService: ConfirmationService
  ) {}

  consulter(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.page.set(1);
    this.compteService.lister(this.tenantSelectionne()).subscribe({
      next: (comptes) => {
        this.comptes.set(comptes);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les comptes de ce tenant.');
        this.loading.set(false);
      },
    });
  }

  basculerStatut(compte: CompteAdmin): void {
    // Confirmation uniquement à la désactivation (audit UX §3.4) : réactiver
    // un compte n'est pas l'action risquée, pas besoin d'un garde-fou.
    if (compte.actif) {
      const confirme = this.confirmationService.confirmer(
        `Désactiver le compte de ${this.libelleActeur(compte)} ? Il ne pourra plus se connecter tant qu'il n'est pas réactivé.`
      );
      if (!confirme) {
        return;
      }
    }
    this.compteService.changerStatut(compte.id, this.tenantSelectionne(), !compte.actif).subscribe({
      next: () => this.consulter(),
      error: () => this.errorMessage.set('Impossible de changer le statut de ce compte.'),
    });
  }

  commencerEditionRoles(compte: CompteAdmin): void {
    this.compteEnEdition.set(compte.id);
    this.rolesEdition.set(new Set(compte.roles));
  }

  annulerEditionRoles(): void {
    this.compteEnEdition.set(null);
  }

  basculerRoleEdition(role: RoleActeur, coche: boolean): void {
    this.rolesEdition.update((roles) => {
      const suivant = new Set(roles);
      if (coche) {
        suivant.add(role);
      } else {
        suivant.delete(role);
      }
      return suivant;
    });
  }

  enregistrerRoles(): void {
    const id = this.compteEnEdition();
    const roles = Array.from(this.rolesEdition());
    if (!id || roles.length === 0) {
      return;
    }
    this.compteService.changerRoles(id, this.tenantSelectionne(), roles).subscribe({
      next: () => {
        this.compteEnEdition.set(null);
        this.consulter();
      },
      error: () => this.errorMessage.set('Impossible de changer les rôles de ce compte.'),
    });
  }

  libelleActeur(compte: CompteAdmin): string {
    if (compte.raisonSociale) {
      return compte.raisonSociale;
    }
    const nomComplet = [compte.prenom, compte.nom].filter(Boolean).join(' ');
    return nomComplet || compte.telephone;
  }
}
