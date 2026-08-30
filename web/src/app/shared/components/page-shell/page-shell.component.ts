import { Component, Input } from '@angular/core';
import { FcResponsiveTableDirective } from '../../directives/fc-responsive-table.directive';

/**
 * Racine `.fc-page` centralisée (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §0.4/2.2) : jusqu'ici
 * chaque écran feature réécrivait `<main class="fc-page">...</main>` dans
 * son propre template (12 occurrences) — une évolution de la structure de
 * page (breadcrumb, largeur adaptative par écran) demandait de modifier
 * chaque fichier individuellement. Un seul point de vérité désormais.
 *
 * `extraClass` couvre les quelques écrans qui ajoutaient une classe de
 * scoping propre au composant à côté de `fc-page` (ex. `kyc-dashboard`)
 * pour leurs styles locaux — appliquée sur le même élément `<main>`, jamais
 * sur le tag hôte `<app-page-shell>`.
 */
@Component({
  selector: 'app-page-shell',
  standalone: true,
  hostDirectives: [FcResponsiveTableDirective],
  template: `<main [class]="'fc-page' + (extraClass ? ' ' + extraClass : '')"><ng-content></ng-content></main>`,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      flex: 1 1 auto;
      width: 100%;
      min-height: 100%;
    }
    :host > main {
      flex: 1 1 auto;
      width: 100%;
      min-height: 100%;
    }
  `,
})
export class PageShellComponent {
  @Input() extraClass?: string;
}
