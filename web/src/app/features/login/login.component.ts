import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { HOME_ROUTE_BY_ROLE } from '../../core/auth/auth.models';
import { BrandLogoComponent } from '../../shared/components/brand-logo/brand-logo.component';

/**
 * FE-WEB-01 : écran de connexion unique (téléphone + code), aucune indication
 * de rôle avant authentification réussie — un seul formulaire pour les trois
 * rôles, la redirection se décide après coup depuis le token (FE-WEB-02).
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, BrandLogoComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  readonly phone = signal('');
  readonly code = signal('');
  readonly errorMessage = signal<string | null>(null);
  readonly submitting = signal(false);

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  submit(): void {
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.authService.login({ phone: this.phone(), code: this.code() }).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.router.navigateByUrl(HOME_ROUTE_BY_ROLE[response.role]);
      },
      error: () => {
        this.submitting.set(false);
        this.errorMessage.set('Numéro de téléphone ou code invalide.');
      },
    });
  }
}
