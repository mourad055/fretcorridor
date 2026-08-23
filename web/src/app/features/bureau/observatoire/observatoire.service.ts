import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AlerteSeuil, Comparateur, EtatAlerte, Indicateur, ObservatoireAxe } from './observatoire.models';

/**
 * EF-BUR-03/04/05/07 : observatoire de marché et alertes de seuil (Sprint 5).
 * Le backend (AlerteSeuilController, MissionAppparieeController#observatoire)
 * est réel et déjà branché sur service-bur — seul l'écran manquait (audit UX
 * 2026-08-23, docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.3/1.4).
 */
@Injectable({ providedIn: 'root' })
export class ObservatoireService {
  constructor(private readonly http: HttpClient) {}

  observatoirePourAxe(axeId: string): Observable<ObservatoireAxe> {
    return this.http.get<ObservatoireAxe>(`${environment.apiBaseUrl}/bureau/observatoire/${axeId}`);
  }

  definirEstimationMarche(axeId: string, volumeMensuelEstime: number, source: string): Observable<void> {
    return this.http.put<void>(`${environment.apiBaseUrl}/bureau/observatoire/${axeId}/estimation-marche`, {
      volumeMensuelEstime,
      source,
    });
  }

  listerAlertes(): Observable<AlerteSeuil[]> {
    return this.http.get<AlerteSeuil[]>(`${environment.apiBaseUrl}/bureau/alertes`);
  }

  etatAlertes(): Observable<EtatAlerte[]> {
    return this.http.get<EtatAlerte[]>(`${environment.apiBaseUrl}/bureau/alertes/etat`);
  }

  configurerAlerte(
    axeId: string,
    indicateur: Indicateur,
    comparateur: Comparateur,
    seuil: number
  ): Observable<AlerteSeuil> {
    return this.http.post<AlerteSeuil>(`${environment.apiBaseUrl}/bureau/alertes`, {
      axeId,
      indicateur,
      comparateur,
      seuil,
    });
  }

  supprimerAlerte(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/bureau/alertes/${id}`);
  }
}
