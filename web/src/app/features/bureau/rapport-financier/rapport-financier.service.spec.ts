import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RapportFinancierService } from './rapport-financier.service';
import { environment } from '../../../../environments/environment';

describe('RapportFinancierService', () => {
  let service: RapportFinancierService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RapportFinancierService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches the financial report of the current tenant', () => {
    let result: unknown;
    service.rapport().subscribe((ecritures) => (result = ecritures));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/rapport-financier`);
    expect(req.request.method).toBe('GET');
    req.flush([]);

    expect(result).toEqual([]);
  });
});
