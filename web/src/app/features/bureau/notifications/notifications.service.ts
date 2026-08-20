import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { NotificationItem } from '../../../shared/models/notification.models';

/** Sprint 9 : centre de notifications du tenant (supervision Bureau, ENF-MUL-01). */
@Injectable({ providedIn: 'root' })
export class NotificationsService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(`${environment.apiBaseUrl}/bureau/notifications`);
  }
}
