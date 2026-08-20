import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CapaciteService } from './capacite.service';
import { environment } from '../../../../environments/environment';

describe('CapaciteService', () => {
  let service: CapaciteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CapaciteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the capacities of the current transporteur', () => {
    let result: unknown;
    service.list().subscribe((capacites) => (result = capacites));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/capacites`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 'cap-1',
        vehicule: 'Camion 10T',
        origine: 'Douala',
        destination: 'Yaoundé',
        departLe: '2026-01-01T00:00:00Z',
        poidsTaxableKg: 9500,
        modeCollecte: 'PORTE_A_PORTE',
        etat: 'PUBLIEE',
      },
    ]);

    expect(result).toHaveLength(1);
  });
});
