import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Mission } from '../../../shared/models/mission.models';

/** Sprint 7 : chronologie des propres missions de l'acteur (PRD §5.3, périmètre strict par acteur). */
@Injectable({ providedIn: 'root' })
export class TransporteurMissionsService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<Mission[]> {
    return this.http.get<Mission[]>(`${environment.apiBaseUrl}/transporteur/missions`);
  }
}
