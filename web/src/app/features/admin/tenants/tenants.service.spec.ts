import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TenantsService } from './tenants.service';
import { environment } from '../../../../environments/environment';

describe('TenantsService', () => {
  let service: TenantsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TenantsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists tenants', () => {
    let result: unknown;
    service.lister().subscribe((tenants) => (result = tenants));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`);
    expect(req.request.method).toBe('GET');
    req.flush([]);

    expect(result).toEqual([]);
  });

  it('creates a tenant', () => {
    service.creer('tenant-new', 'Bureau Neuf', 'Tchad').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ id: 'tenant-new', nom: 'Bureau Neuf', pays: 'Tchad' });
    req.flush({});
  });
});
