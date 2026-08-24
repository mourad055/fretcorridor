import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PageShellComponent } from '../../../shared/components/page-shell/page-shell.component';
import { NotificationAdminService } from './notification-admin.service';
import { NotificationAdmin } from './notification-admin.models';

/**
 * Centre de notifications internes Admin (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §2.6).
 */
@Component({
  selector: 'app-notifications-internes',
  standalone: true,
  imports: [CommonModule, RouterLink, PageShellComponent],
  templateUrl: './notifications-internes.component.html',
})
export class NotificationsInternesComponent implements OnInit {
  readonly alertes = signal<NotificationAdmin[] | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

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
