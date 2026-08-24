import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

/**
 * S18 (Sprint 18, "Second tenant institutionnel") : un opérateur du second
 * bureau invite un transporteur/chauffeur existant — l'invitation EST la
 * validation, aucun flux d'acceptation côté transporteur (règle produit).
 */
@Injectable({ providedIn: 'root' })
export class AffiliationService {
  constructor(private readonly http: HttpClient) {}

  inviter(telephone: string): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/bureau/affiliations`, { telephone });
  }
}
