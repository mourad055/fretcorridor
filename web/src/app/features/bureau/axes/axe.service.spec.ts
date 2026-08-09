import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AxeService } from './axe.service';
import { environment } from '../../../../environments/environment';

describe('AxeService', () => {
  let service: AxeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AxeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the axes of the current tenant', () => {
    let result: unknown;
    service.list().subscribe((axes) => (result = axes));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/axes`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'axe-1', origine: 'Douala', destination: 'Yaoundé', distanceKm: 300, visibiliteActive: true, matchingActif: true, paiementActif: true }]);

    expect(result).toHaveLength(1);
  });
});
