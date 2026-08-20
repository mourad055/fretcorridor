import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { BureauChronologieService } from './bureau-chronologie.service';
import { environment } from '../../../../environments/environment';

describe('BureauChronologieService', () => {
  let service: BureauChronologieService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BureauChronologieService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the missions of the current tenant with their steps', () => {
    let result: unknown;
    service.list().subscribe((missions) => (result = missions));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-chronologie`);
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
