import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RapportFinancierAdminService } from './rapport-financier-admin.service';
import { environment } from '../../../../environments/environment';

describe('RapportFinancierAdminService', () => {
  let service: RapportFinancierAdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RapportFinancierAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches the financial report of the selected tenant', () => {
    let result: unknown;
    service.rapport('tenant-bgft-tchad').subscribe((ecritures) => (result = ecritures));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/rapport-financier/tenant-bgft-tchad`);
    expect(req.request.method).toBe('GET');
    req.flush([]);

    expect(result).toEqual([]);
  });
});
