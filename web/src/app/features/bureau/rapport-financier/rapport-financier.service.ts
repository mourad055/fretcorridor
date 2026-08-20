import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Ecriture } from '../../../shared/models/ecriture.models';
import { DeclarationEspeces } from '../../../shared/models/declaration-especes.models';

/** Rapport financier (lecture seule) du tenant courant, Sprint 8. */
@Injectable({ providedIn: 'root' })
export class RapportFinancierService {
  constructor(private readonly http: HttpClient) {}

  rapport(): Observable<Ecriture[]> {
    return this.http.get<Ecriture[]>(`${environment.apiBaseUrl}/bureau/rapport-financier`);
  }

  /** EF-PAY-07 (S) : missions payées en espèces du territoire, sans protection de la plateforme. */
  paiementsEspeces(): Observable<DeclarationEspeces[]> {
    return this.http.get<DeclarationEspeces[]>(`${environment.apiBaseUrl}/bureau/paiements-especes`);
  }
}
