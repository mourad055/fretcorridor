import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AffiliationService } from './affiliation.service';
import { environment } from '../../../../environments/environment';

describe('AffiliationService', () => {
  let service: AffiliationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AffiliationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('invite un transporteur par telephone', () => {
    service.inviter('+237690000001').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/affiliations`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ telephone: '+237690000001' });
    req.flush(null, { status: 201, statusText: 'Created' });
  });
});
