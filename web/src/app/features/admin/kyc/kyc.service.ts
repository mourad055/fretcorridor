import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { generateIdempotencyKey } from '../../../core/idempotency-key';
import { KycDossier, KycStatut } from './kyc.models';

/** FE-ADM-06 : lecture/action sur la file de vérification KYC via la gateway. */
@Injectable({ providedIn: 'root' })
export class KycService {
  constructor(private readonly http: HttpClient) {}

  listPending(): Observable<KycDossier[]> {
    return this.http.get<KycDossier[]>(`${environment.apiBaseUrl}/admin/kyc/pending`);
  }

  decide(dossierId: string, decision: Extract<KycStatut, 'VALIDE' | 'REJETE'>): Observable<KycDossier> {
    return this.http.post<KycDossier>(
      `${environment.apiBaseUrl}/admin/kyc/${dossierId}/decision`,
      { decision },
      { headers: { 'X-Idempotency-Key': generateIdempotencyKey() } }
    );
  }
}
