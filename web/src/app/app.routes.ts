import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { LoginComponent } from './features/login/login.component';
import { ForbiddenComponent } from './features/forbidden/forbidden.component';
import { BureauHomeComponent } from './features/bureau/bureau-home.component';
import { TransporteurHomeComponent } from './features/transporteur/transporteur-home.component';
import { KycDashboardComponent } from './features/admin/kyc/kyc-dashboard.component';
import { roleGuard, guestGuard } from './core/auth/role.guard';
import { AuthService } from './core/auth/auth.service';
import { HOME_ROUTE_BY_ROLE } from './core/auth/auth.models';

/** Route racine : redirige vers le feature module du rôle courant, ou vers /login. */
const rootRedirectGuard: CanActivateFn = () => {
  const router = inject(Router);
  const session = inject(AuthService).session();
  return router.createUrlTree([session ? HOME_ROUTE_BY_ROLE[session.role] : '/login']);
};

export const routes: Routes = [
  { path: '', component: LoginComponent, canActivate: [rootRedirectGuard] },
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: '403', component: ForbiddenComponent },
  { path: 'bureau', component: BureauHomeComponent, canActivate: [roleGuard], data: { role: 'BUREAU' } },
  {
    path: 'transporteur',
    component: TransporteurHomeComponent,
    canActivate: [roleGuard],
    data: { role: 'TRANSPORTEUR' },
  },
  { path: 'admin', component: KycDashboardComponent, canActivate: [roleGuard], data: { role: 'ADMIN' } },
  { path: '**', redirectTo: 'login' },
];
