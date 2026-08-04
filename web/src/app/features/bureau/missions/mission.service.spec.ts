import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MissionService } from './mission.service';
import { environment } from '../../../../environments/environment';

describe('MissionService', () => {
  let service: MissionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MissionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the matched missions of the current tenant', () => {
    let result: unknown;
    service.list().subscribe((missions) => (result = missions));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 'mission-1',
        transporteurNom: 'Transport Étoile SARL',
        origine: 'Douala',
        destination: 'Yaoundé',
        enlevementLe: '2026-01-01T00:00:00Z',
        statut: 'CONFIRMEE',
      },
    ]);

    expect(result).toHaveLength(1);
  });
});
