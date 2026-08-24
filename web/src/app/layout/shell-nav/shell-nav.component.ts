import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { OngletNav, ongletsPourRole } from '../nav-onglets';

/**
 * Navigation par onglets (Sprint 14) : chaque rôle est éclaté en plusieurs
 * pages plutôt qu'une seule page empilant tous les écrans — voir docs/adr.
 *
 * Réservée au petit écran depuis l'ajout de la sidebar (audit UX
 * 2026-08-23, §2.1) : `ShellComponent` bascule entre celle-ci et
 * `ShellSidebarComponent` par media query CSS, aucun changement de
 * comportement pour ce composant lui-même.
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

  readonly onglets = computed((): OngletNav[] => ongletsPourRole(this.authService.session()?.role));
}
