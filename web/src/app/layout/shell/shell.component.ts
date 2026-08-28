import { Component, computed, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { BrandLogoComponent } from '../../shared/components/brand-logo/brand-logo.component';
import { LangueSwitchComponent } from '../../shared/components/langue-switch/langue-switch.component';
import { ShellNavComponent } from '../shell-nav/shell-nav.component';
import { ShellSidebarComponent } from '../shell-sidebar/shell-sidebar.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { cleLibelleTenant } from '../../shared/utils/libelle-tenant';

/**
 * Enveloppe partagée des 3 rôles (Sprint 11) — en-tête sticky, logo, tenant,
 * déconnexion. Navigation par onglets ajoutée au Sprint 14. Sélecteur de
 * langue déplacé ici depuis Login au Sprint 22 (i18n).
 *
 * Sidebar desktop ajoutée (audit UX 2026-08-23, §2.1) : `ShellSidebarComponent`
 * et `ShellNavComponent` coexistent dans le DOM, la bascule entre les deux se
 * fait par media query CSS (shell.component.css) — jamais par condition
 * Angular, pour rester cohérent en SSR/print et éviter tout flash au resize.
 */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, BrandLogoComponent, LangueSwitchComponent, ShellNavComponent, ShellSidebarComponent, ConfirmDialogComponent, TranslatePipe],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css',
})
export class ShellComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly session = this.authService.session;
  readonly roleLabelKey = computed(() => {
    switch (this.session()?.role) {
      case 'BUREAU':
        return 'shell.role.bureau';
      case 'TRANSPORTEUR':
        return 'shell.role.transporteur';
      case 'ADMIN':
        return 'shell.role.admin';
      default:
        return '';
    }
  });
  readonly tenantLabel = computed(() => {
    const tenantId = this.session()?.tenantId;
    if (!tenantId) {
      return '';
    }
    const cle = cleLibelleTenant(tenantId);
    return cle ? this.translate.instant(cle) : tenantId;
  });

  logout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
