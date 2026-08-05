import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Ecriture } from '../../../shared/models/ecriture.models';

/** Rapport financier (lecture seule) du tenant courant, Sprint 8. */
@Injectable({ providedIn: 'root' })
export class RapportFinancierService {
  constructor(private readonly http: HttpClient) {}

  rapport(): Observable<Ecriture[]> {
    return this.http.get<Ecriture[]>(`${environment.apiBaseUrl}/bureau/rapport-financier`);
  }
}
