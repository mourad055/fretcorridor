import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Position } from './position.models';

/** FE-TRK-04 : lecture des positions du tenant courant via la gateway. */
@Injectable({ providedIn: 'root' })
export class PositionService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<Position[]> {
    return this.http.get<Position[]>(`${environment.apiBaseUrl}/bureau/positions`);
  }
}
