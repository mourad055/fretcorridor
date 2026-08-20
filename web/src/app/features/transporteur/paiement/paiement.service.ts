import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SoldeTransporteur } from './paiement.models';

/** FE-TRP-03 : solde et historique de paiement de l'acteur courant, strictement ses propres écritures. */
@Injectable({ providedIn: 'root' })
export class PaiementService {
  constructor(private readonly http: HttpClient) {}

  solde(): Observable<SoldeTransporteur> {
    return this.http.get<SoldeTransporteur>(`${environment.apiBaseUrl}/transporteur/paiement`);
  }
}
