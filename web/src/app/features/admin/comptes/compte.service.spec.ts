import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CompteService } from './compte.service';
import { environment } from '../../../../environments/environment';

describe('CompteService', () => {
  let service: CompteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CompteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('liste les comptes du tenant demande', () => {
    let result: unknown;
    service.lister('tenant-bgft-douala').subscribe((comptes) => (result = comptes));

    const req = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes` && r.params.get('tenantId') === 'tenant-bgft-douala');
    expect(req.request.method).toBe('GET');
    req.flush([]);

    expect(result).toEqual([]);
  });

  it('change le statut d\'un compte', () => {
    service.changerStatut('c1', 'tenant-bgft-douala', false).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes/c1/statut` && r.params.get('tenantId') === 'tenant-bgft-douala');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ actif: false });
    req.flush({});
  });

  it('change les roles d\'un compte', () => {
    service.changerRoles('c1', 'tenant-bgft-douala', ['ADMINISTRATION']).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes/c1/roles` && r.params.get('tenantId') === 'tenant-bgft-douala');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ roles: ['ADMINISTRATION'] });
    req.flush({});
  });
});
