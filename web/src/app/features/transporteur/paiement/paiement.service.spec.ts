import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { PaiementService } from './paiement.service';
import { environment } from '../../../../environments/environment';

describe('PaiementService', () => {
  let service: PaiementService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PaiementService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches the solde and historique of the current transporteur', () => {
    let result: unknown;
    service.solde().subscribe((solde) => (result = solde));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/paiement`);
    expect(req.request.method).toBe('GET');
    req.flush({ solde: 150, historique: [] });

    expect(result).toEqual({ solde: 150, historique: [] });
  });
});
