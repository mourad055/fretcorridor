import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Configuration } from '../../../shared/models/configuration.models';

/** FE-ADM-03 (Sprint 10) : console de configuration versionnée et auditée. */
@Injectable({ providedIn: 'root' })
export class ConfigurationsService {
  constructor(private readonly http: HttpClient) {}

  /** EF-ADM-06 : catalogue de tous les paramètres métier déjà configurés (pas besoin de connaître la clé à l'avance). */
  catalogue(): Observable<Configuration[]> {
    return this.http.get<Configuration[]>(`${environment.apiBaseUrl}/admin/configurations`);
  }

  historique(cle: string): Observable<Configuration[]> {
    return this.http.get<Configuration[]>(`${environment.apiBaseUrl}/admin/configurations/${cle}/historique`);
  }

  definir(cle: string, valeur: string): Observable<Configuration> {
    return this.http.put<Configuration>(`${environment.apiBaseUrl}/admin/configurations/${cle}`, { valeur });
  }
}
