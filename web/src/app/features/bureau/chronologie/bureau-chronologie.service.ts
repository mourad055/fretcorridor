import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Mission } from '../../../shared/models/mission.models';

/** Sprint 7 : chronologie des missions du territoire (supervision Bureau, ENF-MUL-01). */
@Injectable({ providedIn: 'root' })
export class BureauChronologieService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<Mission[]> {
    return this.http.get<Mission[]>(`${environment.apiBaseUrl}/bureau/missions-chronologie`);
  }
}
