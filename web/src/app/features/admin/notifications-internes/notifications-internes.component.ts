import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import {
  StatusBadgeComponent,
  dossierStatusVariant,
  libelleDossierStatut,
  libellePrioriteDossier,
  libelleTypeDossier,
  type StatusBadgeVariant,
} from '../../../shared/components/status-badge/status-badge.component';
import { NotificationAdminService } from './notification-admin.service';
import { NotificationAdmin } from './notification-admin.models';

/**
 * Centre de notifications internes Admin (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.6).
 */
@Component({
  selector: 'app-notifications-internes',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe, PageShellComponent, StatusBadgeComponent],
  templateUrl: './notifications-internes.component.html',
  styleUrl: './notifications-internes.component.css',
})
export class NotificationsInternesComponent implements OnInit {
  readonly alertes = signal<NotificationAdmin[] | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly libelleTypeDossier = libelleTypeDossier;
  readonly libellePrioriteDossier = libellePrioriteDossier;
  readonly libelleDossierStatut = libelleDossierStatut;
  readonly dossierStatusVariant = dossierStatusVariant;
  readonly prioriteVariant = prioriteDossierVariant;

  constructor(private readonly notificationAdminService: NotificationAdminService) {}

  ngOnInit(): void {
    this.notificationAdminService.dossiersEnRetard().subscribe({
      next: (alertes) => {
        this.alertes.set(alertes);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les notifications.');
        this.loading.set(false);
      },
    });
  }
}

function prioriteDossierVariant(priorite?: string): StatusBadgeVariant {
  switch (priorite) {
    case 'HAUTE':
      return 'danger';
    case 'NORMALE':
      return 'warning';
    default:
      return 'neutral';
  }
}
