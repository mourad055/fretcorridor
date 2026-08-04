import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Capacite } from './capacite.models';

/** FE-TRP-01 : lecture des capacités déclarées par l'acteur courant via la gateway. */
@Injectable({ providedIn: 'root' })
export class CapaciteService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<Capacite[]> {
    return this.http.get<Capacite[]>(`${environment.apiBaseUrl}/transporteur/capacites`);
  }
}
