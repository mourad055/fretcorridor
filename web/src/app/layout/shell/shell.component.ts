import { Component, computed, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { BrandLogoComponent } from '../../shared/components/brand-logo/brand-logo.component';
import { LangueSwitchComponent } from '../../shared/components/langue-switch/langue-switch.component';
import { ShellNavComponent } from '../shell-nav/shell-nav.component';

/**
 * Enveloppe partagée des 3 rôles (Sprint 11) — en-tête sticky, logo, tenant,
 * déconnexion. Navigation par onglets ajoutée au Sprint 14. Sélecteur de
 * langue déplacé ici depuis Login au Sprint 22 (i18n).
 */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, BrandLogoComponent, LangueSwitchComponent, ShellNavComponent, TranslatePipe],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css',
})
export class ShellComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

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

  logout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
