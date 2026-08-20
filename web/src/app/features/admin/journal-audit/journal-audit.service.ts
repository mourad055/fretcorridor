import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { EntreeJournalAudit } from '../../../shared/models/journal-audit.models';

/** FE-ADM-05 (Sprint 10) : journal d'audit consultable et exportable, en lecture seule, append-only. */
@Injectable({ providedIn: 'root' })
export class JournalAuditService {
  constructor(private readonly http: HttpClient) {}

  lister(tenantId?: string): Observable<EntreeJournalAudit[]> {
    return this.http.get<EntreeJournalAudit[]>(`${environment.apiBaseUrl}/admin/journal-audit`, {
      params: tenantId ? { tenantId } : {},
    });
  }

  exporterCsv(tenantId?: string): Observable<string> {
    return this.http.get(`${environment.apiBaseUrl}/admin/journal-audit/export`, {
      params: tenantId ? { tenantId } : {},
      responseType: 'text',
    });
  }
}
