import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

interface OngletNav {
  path: string;
  label: string;
  exact: boolean;
}

const ONGLETS_BUREAU: OngletNav[] = [
  { path: '/bureau', label: 'Axes', exact: true },
  { path: '/bureau/missions', label: 'Missions appariées', exact: false },
  { path: '/bureau/positions', label: 'Suivi temps réel', exact: false },
  { path: '/bureau/chronologie', label: 'Chronologie', exact: false },
  { path: '/bureau/rapport-financier', label: 'Rapport financier', exact: false },
  { path: '/bureau/notifications', label: 'Notifications', exact: false },
];

const ONGLETS_TRANSPORTEUR: OngletNav[] = [
  { path: '/transporteur', label: 'Capacités', exact: true },
  { path: '/transporteur/missions', label: 'Mes missions', exact: false },
  { path: '/transporteur/paiement', label: 'Paiement', exact: false },
];

const ONGLETS_ADMIN: OngletNav[] = [
  { path: '/admin', label: 'KYC', exact: true },
  { path: '/admin/rapport-financier', label: 'Rapport financier', exact: false },
  { path: '/admin/dossiers', label: 'Dossiers', exact: false },
  { path: '/admin/configurations', label: 'Configuration', exact: false },
  { path: '/admin/tenants', label: 'Tenants', exact: false },
  { path: '/admin/journal-audit', label: "Journal d'audit", exact: false },
];

/**
 * Navigation par onglets (Sprint 14) : chaque rôle est éclaté en plusieurs
 * pages plutôt qu'une seule page empilant tous les écrans — voir docs/adr.
 */
@Component({
  selector: 'app-shell-nav',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './shell-nav.component.html',
  styleUrl: './shell-nav.component.css',
})
export class ShellNavComponent {
  private readonly authService = inject(AuthService);

  readonly onglets = computed((): OngletNav[] => {
    switch (this.authService.session()?.role) {
      case 'BUREAU':
        return ONGLETS_BUREAU;
      case 'TRANSPORTEUR':
        return ONGLETS_TRANSPORTEUR;
      case 'ADMIN':
        return ONGLETS_ADMIN;
      default:
        return [];
    }
  });
}
