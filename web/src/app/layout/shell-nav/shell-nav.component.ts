import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';

interface OngletNav {
  path: string;
  labelKey: string;
  exact: boolean;
}

const ONGLETS_BUREAU: OngletNav[] = [
  { path: '/bureau', labelKey: 'nav.bureau.axes', exact: true },
  { path: '/bureau/missions', labelKey: 'nav.bureau.missions', exact: false },
  { path: '/bureau/positions', labelKey: 'nav.bureau.positions', exact: false },
  { path: '/bureau/chronologie', labelKey: 'nav.bureau.chronologie', exact: false },
  { path: '/bureau/rapport-financier', labelKey: 'nav.bureau.rapportFinancier', exact: false },
  { path: '/bureau/notifications', labelKey: 'nav.bureau.notifications', exact: false },
  { path: '/bureau/observatoire', labelKey: 'nav.bureau.observatoire', exact: false },
];

const ONGLETS_TRANSPORTEUR: OngletNav[] = [
  { path: '/transporteur', labelKey: 'nav.transporteur.capacites', exact: true },
  { path: '/transporteur/missions', labelKey: 'nav.transporteur.missions', exact: false },
  { path: '/transporteur/paiement', labelKey: 'nav.transporteur.paiement', exact: false },
];

const ONGLETS_ADMIN: OngletNav[] = [
  { path: '/admin', labelKey: 'nav.admin.kyc', exact: true },
  { path: '/admin/rapport-financier', labelKey: 'nav.admin.rapportFinancier', exact: false },
  { path: '/admin/dossiers', labelKey: 'nav.admin.dossiers', exact: false },
  { path: '/admin/configurations', labelKey: 'nav.admin.configuration', exact: false },
  { path: '/admin/tenants', labelKey: 'nav.admin.tenants', exact: false },
  { path: '/admin/journal-audit', labelKey: 'nav.admin.journalAudit', exact: false },
];

/**
 * Navigation par onglets (Sprint 14) : chaque rôle est éclaté en plusieurs
 * pages plutôt qu'une seule page empilant tous les écrans — voir docs/adr.
 */
@Component({
  selector: 'app-shell-nav',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, TranslatePipe],
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
