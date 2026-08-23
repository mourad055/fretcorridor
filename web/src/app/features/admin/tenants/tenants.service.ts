import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Tenant } from '../../../shared/models/tenant.models';

/** FE-ADM-04 (Sprint 10) : gestion des tenants (bureaux de fret, entités multi-tenant). */
@Injectable({ providedIn: 'root' })
export class TenantsService {
  constructor(private readonly http: HttpClient) {}

  lister(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(`${environment.apiBaseUrl}/admin/tenants`);
  }

  creer(id: string, nom: string, pays: string): Observable<Tenant> {
    return this.http.post<Tenant>(`${environment.apiBaseUrl}/admin/tenants`, { id, nom, pays });
  }

  modifier(id: string, nom: string, pays: string, actif: boolean): Observable<Tenant> {
    return this.http.put<Tenant>(`${environment.apiBaseUrl}/admin/tenants/${id}`, { nom, pays, actif });
  }
}
