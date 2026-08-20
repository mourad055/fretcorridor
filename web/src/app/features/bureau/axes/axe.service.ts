import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Axe } from './axe.models';

/** FE-BUR-01 : lecture des axes du tenant courant via la gateway (ENF-MUL-01). */
@Injectable({ providedIn: 'root' })
export class AxeService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<Axe[]> {
    return this.http.get<Axe[]>(`${environment.apiBaseUrl}/bureau/axes`);
  }
}
