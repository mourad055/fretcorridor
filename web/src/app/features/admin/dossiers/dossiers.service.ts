import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Dossier, DossierConsolide } from '../../../shared/models/dossier.models';

/** FE-ADM-01/02 (Sprint 10) : file de travail priorisée et dossier consolidé. */
@Injectable({ providedIn: 'root' })
export class DossiersService {
  constructor(private readonly http: HttpClient) {}

  fileDeTravail(tenantId: string): Observable<Dossier[]> {
    return this.http.get<Dossier[]>(`${environment.apiBaseUrl}/admin/dossiers`, { params: { tenantId } });
  }

  consulter(dossierId: string): Observable<DossierConsolide> {
    return this.http.get<DossierConsolide>(`${environment.apiBaseUrl}/admin/dossiers/${dossierId}`);
  }

  priseEnCharge(dossierId: string): Observable<Dossier> {
    return this.http.post<Dossier>(`${environment.apiBaseUrl}/admin/dossiers/${dossierId}/prise-en-charge`, {});
  }

  decider(dossierId: string, decision: string, motif: string): Observable<Dossier> {
    return this.http.post<Dossier>(`${environment.apiBaseUrl}/admin/dossiers/${dossierId}/decision`, { decision, motif });
  }

  declencherEscalade(): Observable<Dossier[]> {
    return this.http.post<Dossier[]>(`${environment.apiBaseUrl}/admin/dossiers/escalade`, {});
  }
}
