import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationsService } from './notifications.service';
import { NotificationItem } from '../../../shared/models/notification.models';

/** FE-BUR (Sprint 9) : un Bureau voit ses notifications email dans le centre web. */
@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.css',
})
export class NotificationsComponent implements OnInit {
  readonly notifications = signal<NotificationItem[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor(private readonly notificationsService: NotificationsService) {}

  ngOnInit(): void {
    this.notificationsService.list().subscribe({
      next: (notifications) => {
        this.notifications.set(notifications);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les notifications.');
        this.loading.set(false);
      },
    });
  }
}
