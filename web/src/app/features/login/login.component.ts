import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { HOME_ROUTE_BY_ROLE, Role, TenantOption } from '../../core/auth/auth.models';
import { BrandLogoComponent } from '../../shared/components/brand-logo/brand-logo.component';
import { LangueSwitchComponent } from '../../shared/components/langue-switch/langue-switch.component';
import { environment } from '../../../environments/environment';
import { cleLibelleTenant } from '../../shared/utils/libelle-tenant';

interface DemoAccount {
  labelKey: string;
  phone: string;
}

/**
 * FE-WEB-01 : écran de connexion unique (téléphone + code), aucune indication
 * de rôle avant authentification réussie — un seul formulaire pour les trois
 * rôles, la redirection se décide après coup depuis le token (FE-WEB-02).
 *
 * S18 : si le compte est rattaché à plusieurs tenants (affiliation accordée
 * par un second bureau), un choix s'affiche avant d'entrer dans l'app —
 * identique au flux mobile. Le cas normal (un seul tenant) ne passe jamais
 * par cet écran. Un échec réseau de la liste ne bloque pas la connexion
 * (ENF-DIS-04) : on entre avec le tenant d'origine déjà porté par le JWT.
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
  readonly tenantsAChoisir = signal<TenantOption[] | null>(null);

  readonly enableDemoLogin = environment.enableDemoLogin;
  readonly demoAccounts: DemoAccount[] = [
    { labelKey: 'login.demo.bureau', phone: '+237600000001' },
    { labelKey: 'login.demo.transporteur', phone: '+237696000001' },
    { labelKey: 'login.demo.admin', phone: '+237600000003' },
    { labelKey: 'login.demo.bureauTchad', phone: '+235600000004' },
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly translate: TranslateService
  ) {}

  libelleTenant(tenantId: string): string {
    const cle = cleLibelleTenant(tenantId);
    return cle ? this.translate.instant(cle) : tenantId;
  }

  submit(): void {
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.authService.login({ phone: this.phone(), code: this.code() }).subscribe({
      next: (response) => this.resoudreTenantsPuisNaviguer(response.role),
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

  choisirTenant(tenantId: string): void {
    if (this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.authService.selectionnerTenant(tenantId).subscribe({
      next: (response) => {
        this.submitting.set(false);
        void this.router.navigateByUrl(this.routeAccueil(response.role));
      },
      error: () => {
        this.submitting.set(false);
        this.errorMessage.set('login.tenant.error');
      },
    });
  }

  annulerSelection(): void {
    this.authService.logout();
    this.tenantsAChoisir.set(null);
    this.errorMessage.set(null);
    this.phone.set('');
    this.code.set('');
  }

  private resoudreTenantsPuisNaviguer(role: Role | string): void {
    this.authService.mesTenants().subscribe({
      next: (tenants) => {
        this.submitting.set(false);
        if (tenants.length > 1) {
          this.tenantsAChoisir.set(tenants);
          return;
        }
        void this.router.navigateByUrl(this.routeAccueil(role));
      },
      error: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl(this.routeAccueil(role));
      },
    });
  }

  /** Route d'accueil web — rôles mobiles chauffeur → espace transporteur. */
  private routeAccueil(role: Role | string): string {
    if (role in HOME_ROUTE_BY_ROLE) {
      return HOME_ROUTE_BY_ROLE[role as Role];
    }
    if (role === 'CHAUFFEUR' || role === 'CHAUFFEUR_PROPRIETAIRE') {
      return HOME_ROUTE_BY_ROLE.TRANSPORTEUR;
    }
    return '/login';
  }
}
