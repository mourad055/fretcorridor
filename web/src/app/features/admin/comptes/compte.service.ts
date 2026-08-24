import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CompteAdmin, RoleActeur } from './compte.models';

/**
 * Gestion des comptes par un Admin (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.1) : la donnée vient de
 * service-ida (Mobile, source d'identité unique) via la gateway.
 */
@Injectable({ providedIn: 'root' })
export class CompteService {
  constructor(private readonly http: HttpClient) {}

  lister(tenantId: string): Observable<CompteAdmin[]> {
    return this.http.get<CompteAdmin[]>(`${environment.apiBaseUrl}/admin/comptes`, { params: { tenantId } });
  }

  changerStatut(id: string, tenantId: string, actif: boolean): Observable<CompteAdmin> {
    return this.http.put<CompteAdmin>(`${environment.apiBaseUrl}/admin/comptes/${id}/statut`, { actif }, { params: { tenantId } });
  }

  changerRoles(id: string, tenantId: string, roles: RoleActeur[]): Observable<CompteAdmin> {
    return this.http.put<CompteAdmin>(`${environment.apiBaseUrl}/admin/comptes/${id}/roles`, { roles }, { params: { tenantId } });
  }
}
