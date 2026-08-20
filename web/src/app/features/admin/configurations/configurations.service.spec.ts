import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ConfigurationsService } from './configurations.service';
import { environment } from '../../../../environments/environment';

describe('ConfigurationsService', () => {
  let service: ConfigurationsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ConfigurationsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('gets the catalogue of already configured keys', () => {
    let result: unknown;
    service.catalogue().subscribe((catalogue) => (result = catalogue));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations`);
    expect(req.request.method).toBe('GET');
    req.flush([]);

    expect(result).toEqual([]);
  });

  it('gets the version history of a configuration key', () => {
    let result: unknown;
    service.historique('seuil-agregation-bur').subscribe((historique) => (result = historique));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`);
    expect(req.request.method).toBe('GET');
    req.flush([]);

    expect(result).toEqual([]);
  });

  it('defines a new version of a configuration key', () => {
    service.definir('seuil-agregation-bur', '5').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ valeur: '5' });
    req.flush({});
  });
});
