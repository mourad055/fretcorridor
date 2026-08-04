import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MissionAppariee } from './mission.models';

/** FE-BUR-01 : lecture des missions appariées du tenant courant via la gateway. */
@Injectable({ providedIn: 'root' })
export class MissionService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<MissionAppariee[]> {
    return this.http.get<MissionAppariee[]>(`${environment.apiBaseUrl}/bureau/missions-appariees`);
  }
}
