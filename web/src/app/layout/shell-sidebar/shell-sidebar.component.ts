import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { OngletNav, ongletsPourRole } from '../nav-onglets';

interface GroupeOnglets {
  groupeKey: string | null;
  items: OngletNav[];
}

/**
 * Navigation latérale desktop (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.1) : le reproche
 * "onglets en dessous de la navbar au lieu d'une nav latérale" portait sur
 * `ShellNavComponent` (onglets horizontaux, Sprint 14, toujours en place —
 * conservé comme repli sous 860px). Au-delà, cette sidebar regroupe les
 * mêmes onglets par thème (ex. Admin : Conformité / Finance / Configuration)
 * plutôt qu'une liste plate, en réutilisant `ongletsPourRole` — jamais une
 * seconde source de vérité de la navigation.
 */
@Component({
  selector: 'app-shell-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, TranslatePipe],
  templateUrl: './shell-sidebar.component.html',
  styleUrl: './shell-sidebar.component.css',
})
export class ShellSidebarComponent {
  private readonly authService = inject(AuthService);

  readonly groupes = computed((): GroupeOnglets[] => {
    const onglets = ongletsPourRole(this.authService.session()?.role);
    const groupes: GroupeOnglets[] = [];
    const indexParGroupe = new Map<string | null, number>();

    for (const onglet of onglets) {
      const cle = onglet.groupeKey ?? null;
      let index = indexParGroupe.get(cle);
      if (index === undefined) {
        index = groupes.length;
        indexParGroupe.set(cle, index);
        groupes.push({ groupeKey: cle, items: [] });
      }
      groupes[index].items.push(onglet);
    }

    return groupes;
  });
}
