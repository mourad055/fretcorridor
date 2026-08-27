import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { generateIdempotencyKey } from '../../../core/idempotency-key';
import { KycDetail, KycDossier, KycFiltre, KycStatut } from './kyc.models';

/** FE-ADM-06 : lecture/action sur la file de vérification KYC via la gateway. */
@Injectable({ providedIn: 'root' })
export class KycService {
  constructor(private readonly http: HttpClient) {}

  listPending(tenantId: string): Observable<KycDossier[]> {
    return this.http.get<KycDossier[]>(`${environment.apiBaseUrl}/admin/kyc/pending`, {
      params: { tenantId },
    });
  }

  listByNiveau(tenantId: string, niveau: Exclude<KycFiltre, 'pending'>): Observable<KycDossier[]> {
    return this.http.get<KycDossier[]>(`${environment.apiBaseUrl}/admin/kyc`, {
      params: { tenantId, niveau },
    });
  }

  detail(tenantId: string, dossierId: string): Observable<KycDetail> {
    return this.http.get<KycDetail>(`${environment.apiBaseUrl}/admin/kyc/${dossierId}`, {
      params: { tenantId },
    });
  }

  decide(
    tenantId: string,
    dossierId: string,
    decision: Extract<KycStatut, 'VALIDE' | 'REJETE'>,
    motif?: string
  ): Observable<KycDossier> {
    return this.http.post<KycDossier>(
      `${environment.apiBaseUrl}/admin/kyc/${dossierId}/decision`,
      { decision, motif: motif ?? null },
      {
        params: { tenantId },
        headers: { 'X-Idempotency-Key': generateIdempotencyKey() },
      }
    );
  }

  chargerPiece(tenantId: string, dossierId: string, pieceId: string): Observable<Blob> {
    return this.http.get(`${environment.apiBaseUrl}/admin/kyc/${dossierId}/pieces/${pieceId}/content`, {
      params: { tenantId },
      responseType: 'blob',
    });
  }
}
