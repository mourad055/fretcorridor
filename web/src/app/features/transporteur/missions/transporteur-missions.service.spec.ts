import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TransporteurMissionsService } from './transporteur-missions.service';
import { environment } from '../../../../environments/environment';

describe('TransporteurMissionsService', () => {
  let service: TransporteurMissionsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TransporteurMissionsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the missions of the current transporteur with their steps', () => {
    let result: unknown;
    service.list().subscribe((missions) => (result = missions));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/missions`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 'mission-a',
        transporteurNom: 'Transport Étoile SARL',
        origine: 'Douala',
        destination: 'Yaoundé',
        etapes: [{ rang: 1, type: 'ENLEVEMENT', lieu: 'Douala', etat: 'TERMINEE' }],
      },
    ]);

    expect(result).toHaveLength(1);
  });
});
