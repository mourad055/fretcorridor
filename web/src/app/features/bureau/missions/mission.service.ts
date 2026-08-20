import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MissionAppariee, MissionsFiltre } from './mission.models';

/** FE-BUR-01 : lecture des missions appariées du tenant courant via la gateway. */
@Injectable({ providedIn: 'root' })
export class MissionService {
  constructor(private readonly http: HttpClient) {}

  list(filtre?: MissionsFiltre): Observable<MissionAppariee[]> {
    return this.http.get<MissionAppariee[]>(`${environment.apiBaseUrl}/bureau/missions-appariees`, {
      params: this.paramsDe(filtre),
    });
  }

  detail(missionId: string): Observable<MissionAppariee> {
    return this.http.get<MissionAppariee>(`${environment.apiBaseUrl}/bureau/missions-appariees/${missionId}`);
  }

  /** EF-BUR-02 : export des flux supervisés (mêmes filtres que la liste). */
  exporterCsv(filtre?: MissionsFiltre): Observable<string> {
    return this.http.get(`${environment.apiBaseUrl}/bureau/missions-appariees/export`, {
      params: this.paramsDe(filtre),
      responseType: 'text',
    });
  }

  private paramsDe(filtre?: MissionsFiltre): Record<string, string> {
    const params: Record<string, string> = {};
    if (filtre?.statut) {
      params['statut'] = filtre.statut;
    }
    if (filtre?.axeId) {
      params['axeId'] = filtre.axeId;
    }
    return params;
  }
}
