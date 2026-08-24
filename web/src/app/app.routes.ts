import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { LoginComponent } from './features/login/login.component';
import { ForbiddenComponent } from './features/forbidden/forbidden.component';
import { AxesMapComponent } from './features/bureau/axes/axes-map.component';
import { MissionsListComponent } from './features/bureau/missions/missions-list.component';
import { PositionsListComponent } from './features/bureau/positions/positions-list.component';
import { BureauChronologieComponent } from './features/bureau/chronologie/bureau-chronologie.component';
import { RapportFinancierComponent } from './features/bureau/rapport-financier/rapport-financier.component';
import { NotificationsComponent } from './features/bureau/notifications/notifications.component';
import { ObservatoireComponent } from './features/bureau/observatoire/observatoire.component';
import { CapacitesListComponent } from './features/transporteur/capacites/capacites-list.component';
import { TransporteurMissionsComponent } from './features/transporteur/missions/transporteur-missions.component';
import { PaiementComponent } from './features/transporteur/paiement/paiement.component';
import { KycDashboardComponent } from './features/admin/kyc/kyc-dashboard.component';
import { RapportFinancierAdminComponent } from './features/admin/rapport-financier/rapport-financier-admin.component';
import { DossiersComponent } from './features/admin/dossiers/dossiers.component';
import { ConfigurationsComponent } from './features/admin/configurations/configurations.component';
import { TenantsComponent } from './features/admin/tenants/tenants.component';
import { JournalAuditComponent } from './features/admin/journal-audit/journal-audit.component';
import { ComptesComponent } from './features/admin/comptes/comptes.component';
import { RechercheComponent } from './features/admin/recherche/recherche.component';
import { ShellComponent } from './layout/shell/shell.component';
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
  {
    path: '',
    component: ShellComponent,
    children: [
      // Sprint 14 : chaque rôle est éclaté en onglets (routes enfants) plutôt
      // qu'une seule page empilant tous les écrans. Le premier onglet occupe
      // le chemin exact du rôle (/bureau, /transporteur, /admin) pour ne pas
      // changer HOME_ROUTE_BY_ROLE ni l'URL attendue après connexion.
      { path: 'bureau', component: AxesMapComponent, canActivate: [roleGuard], data: { role: 'BUREAU' } },
      {
        path: 'bureau/missions',
        component: MissionsListComponent,
        canActivate: [roleGuard],
        data: { role: 'BUREAU' },
      },
      {
        path: 'bureau/positions',
        component: PositionsListComponent,
        canActivate: [roleGuard],
        data: { role: 'BUREAU' },
      },
      {
        path: 'bureau/chronologie',
        component: BureauChronologieComponent,
        canActivate: [roleGuard],
        data: { role: 'BUREAU' },
      },
      {
        path: 'bureau/rapport-financier',
        component: RapportFinancierComponent,
        canActivate: [roleGuard],
        data: { role: 'BUREAU' },
      },
      {
        path: 'bureau/notifications',
        component: NotificationsComponent,
        canActivate: [roleGuard],
        data: { role: 'BUREAU' },
      },
      {
        path: 'bureau/observatoire',
        component: ObservatoireComponent,
        canActivate: [roleGuard],
        data: { role: 'BUREAU' },
      },

      {
        path: 'transporteur',
        component: CapacitesListComponent,
        canActivate: [roleGuard],
        data: { role: 'TRANSPORTEUR' },
      },
      {
        path: 'transporteur/missions',
        component: TransporteurMissionsComponent,
        canActivate: [roleGuard],
        data: { role: 'TRANSPORTEUR' },
      },
      {
        path: 'transporteur/paiement',
        component: PaiementComponent,
        canActivate: [roleGuard],
        data: { role: 'TRANSPORTEUR' },
      },

      { path: 'admin', component: KycDashboardComponent, canActivate: [roleGuard], data: { role: 'ADMIN' } },
      {
        path: 'admin/rapport-financier',
        component: RapportFinancierAdminComponent,
        canActivate: [roleGuard],
        data: { role: 'ADMIN' },
      },
      {
        path: 'admin/dossiers',
        component: DossiersComponent,
        canActivate: [roleGuard],
        data: { role: 'ADMIN' },
      },
      {
        path: 'admin/configurations',
        component: ConfigurationsComponent,
        canActivate: [roleGuard],
        data: { role: 'ADMIN' },
      },
      {
        path: 'admin/tenants',
        component: TenantsComponent,
        canActivate: [roleGuard],
        data: { role: 'ADMIN' },
      },
      {
        path: 'admin/journal-audit',
        component: JournalAuditComponent,
        canActivate: [roleGuard],
        data: { role: 'ADMIN' },
      },
      {
        path: 'admin/comptes',
        component: ComptesComponent,
        canActivate: [roleGuard],
        data: { role: 'ADMIN' },
      },
      {
        path: 'admin/recherche',
        component: RechercheComponent,
        canActivate: [roleGuard],
        data: { role: 'ADMIN' },
      },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
