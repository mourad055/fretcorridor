import { Component, computed, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { BrandLogoComponent } from '../../shared/components/brand-logo/brand-logo.component';
import { ShellNavComponent } from '../shell-nav/shell-nav.component';

/**
 * Enveloppe partagée des 3 rôles (Sprint 11) — en-tête sticky, logo, tenant,
 * déconnexion. Navigation par onglets ajoutée au Sprint 14.
 */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, BrandLogoComponent, ShellNavComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css',
})
export class ShellComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly session = this.authService.session;
  readonly roleLabel = computed(() => {
    switch (this.session()?.role) {
      case 'BUREAU':
        return 'Bureau de fret';
      case 'TRANSPORTEUR':
        return 'Transporteur';
      case 'ADMIN':
        return 'Administration';
      default:
        return '';
    }
  });

  logout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
