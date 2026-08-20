import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Ecriture } from '../../../shared/models/ecriture.models';
import { DeclarationEspeces } from '../../../shared/models/declaration-especes.models';

/** Consultation transverse (Sprint 8) : un Admin choisit le tenant à consulter — journalisée côté gateway (ENF-SEC-02). */
@Injectable({ providedIn: 'root' })
export class RapportFinancierAdminService {
  constructor(private readonly http: HttpClient) {}

  rapport(tenantId: string): Observable<Ecriture[]> {
    return this.http.get<Ecriture[]>(`${environment.apiBaseUrl}/admin/rapport-financier/${tenantId}`);
  }

  /** EF-PAY-07 (S) : missions payées en espèces du tenant consulté, sans protection de la plateforme. */
  paiementsEspeces(tenantId: string): Observable<DeclarationEspeces[]> {
    return this.http.get<DeclarationEspeces[]>(`${environment.apiBaseUrl}/admin/paiements-especes/${tenantId}`);
  }
}
