import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { CompteService } from './compte.service';
import { CompteAdmin, ROLES_ACTEUR, RoleActeur } from './compte.models';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { paginer } from '../../../shared/utils/pagination';
import { TENANTS_ADMIN, TENANT_ADMIN_DEFAUT } from '../admin-tenants';

const TAILLE_PAGE = 20;

const ROLES_KYC = new Set<RoleActeur>(['CHAUFFEUR', 'TRANSPORTEUR', 'CHAUFFEUR_PROPRIETAIRE']);

/**
 * Administration des comptes issus de service-ida (inscriptions mobile + web).
 * Le CDC place les opérations chargeur sur l'app Client ; le portail web Admin
 * supervise identité, rôles, statut et renvoie vers la file KYC.
 */
@Component({
  selector: 'app-comptes',
  standalone: true,
  imports: [CommonModule, PageShellComponent, FormsModule, PaginationComponent, RouterLink],
  templateUrl: './comptes.component.html',
  styleUrl: './comptes.component.css',
})
export class ComptesComponent implements OnInit {
  readonly tenants = TENANTS_ADMIN;
  readonly tenantSelectionne = signal<string>(TENANT_ADMIN_DEFAUT);
  readonly comptes = signal<CompteAdmin[] | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly rolesDisponibles = ROLES_ACTEUR;

  readonly compteConsulte = signal<CompteAdmin | null>(null);
  readonly compteRolesEdition = signal<CompteAdmin | null>(null);
  readonly rolesEdition = signal<Set<RoleActeur>>(new Set());
  readonly confirmationStatut = signal<CompteAdmin | null>(null);

  readonly page = signal(1);
  readonly taillePage = TAILLE_PAGE;
  readonly comptesAffiches = computed(() => paginer(this.comptes() ?? [], this.page(), this.taillePage));

  constructor(private readonly compteService: CompteService) {}

  ngOnInit(): void {
    this.consulter();
  }

  consulter(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.page.set(1);
    this.fermerModales();
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

  voirCompte(compte: CompteAdmin): void {
    this.compteConsulte.set(compte);
  }

  fermerConsultation(): void {
    this.compteConsulte.set(null);
  }

  demanderDesactivation(compte: CompteAdmin): void {
    if (!compte.actif) {
      this.confirmationStatut.set(compte);
      return;
    }
    this.confirmationStatut.set(compte);
  }

  annulerDesactivation(): void {
    this.confirmationStatut.set(null);
  }

  confirmerDesactivation(): void {
    const compte = this.confirmationStatut();
    if (!compte) {
      return;
    }
    this.confirmationStatut.set(null);
    this.executerChangementStatut(compte, !compte.actif);
  }

  ouvrirEditionRoles(compte: CompteAdmin): void {
    this.compteRolesEdition.set(compte);
    this.rolesEdition.set(new Set(compte.roles));
  }

  annulerEditionRoles(): void {
    this.compteRolesEdition.set(null);
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
    const compte = this.compteRolesEdition();
    const roles = Array.from(this.rolesEdition());
    if (!compte || roles.length === 0) {
      return;
    }
    this.compteService.changerRoles(compte.id, this.tenantSelectionne(), roles).subscribe({
      next: () => {
        this.compteRolesEdition.set(null);
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

  canalMobile(compte: CompteAdmin): string {
    if (compte.roles.includes('CHARGEUR') && !this.aRoleTransport(compte)) {
      return 'App Client mobile (publication de demandes, propositions, suivi)';
    }
    if (this.aRoleTransport(compte)) {
      return 'App Chauffeur / Transporteur mobile (capacités, missions, exécution)';
    }
    if (compte.roles.includes('BUREAU')) {
      return 'Portail web Bureau de fret';
    }
    if (compte.roles.includes('ADMINISTRATION')) {
      return 'Portail web Administration';
    }
    return 'Non renseigné';
  }

  eligibleKyc(compte: CompteAdmin): boolean {
    return compte.roles.some((role) => ROLES_KYC.has(role));
  }

  queryKyc(): { tenantId: string } {
    return { tenantId: this.tenantSelectionne() };
  }

  private aRoleTransport(compte: CompteAdmin): boolean {
    return compte.roles.some((role) =>
      role === 'CHAUFFEUR' || role === 'TRANSPORTEUR' || role === 'CHAUFFEUR_PROPRIETAIRE'
    );
  }

  private executerChangementStatut(compte: CompteAdmin, actif: boolean): void {
    this.compteService.changerStatut(compte.id, this.tenantSelectionne(), actif).subscribe({
      next: () => this.consulter(),
      error: () => this.errorMessage.set('Impossible de changer le statut de ce compte.'),
    });
  }

  private fermerModales(): void {
    this.compteConsulte.set(null);
    this.compteRolesEdition.set(null);
    this.confirmationStatut.set(null);
  }
}
