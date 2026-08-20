import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { HOME_ROUTE_BY_ROLE } from '../../core/auth/auth.models';
import { BrandLogoComponent } from '../../shared/components/brand-logo/brand-logo.component';
import { LangueSwitchComponent } from '../../shared/components/langue-switch/langue-switch.component';
import { environment } from '../../../environments/environment';

interface DemoAccount {
  labelKey: string;
  phone: string;
}

/**
 * FE-WEB-01 : écran de connexion unique (téléphone + code), aucune indication
 * de rôle avant authentification réussie — un seul formulaire pour les trois
 * rôles, la redirection se décide après coup depuis le token (FE-WEB-02).
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, BrandLogoComponent, LangueSwitchComponent, TranslatePipe],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  readonly phone = signal('');
  readonly code = signal('');
  readonly errorMessage = signal<string | null>(null);
  readonly submitting = signal(false);

  readonly enableDemoLogin = environment.enableDemoLogin;
  readonly demoAccounts: DemoAccount[] = [
    { labelKey: 'login.demo.bureau', phone: '+237600000001' },
    { labelKey: 'login.demo.transporteur', phone: '+237600000002' },
    { labelKey: 'login.demo.admin', phone: '+237600000003' },
    { labelKey: 'login.demo.bureauTchad', phone: '+235600000004' },
  ];

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
        this.errorMessage.set('login.error');
      },
    });
  }

  loginAsDemo(account: DemoAccount): void {
    if (this.submitting()) {
      return;
    }
    this.phone.set(account.phone);
    this.code.set('1234');
    this.submit();
  }
}
