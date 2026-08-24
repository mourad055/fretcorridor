import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

function fakeToken(role: string, sub = 'actor-1'): string {
  const base64url = (obj: object) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url({ alg: 'HS256' })}.${base64url({ sub, role })}.sig`;
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('starts unauthenticated when sessionStorage is empty', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.session()).toBeNull();
  });

  it('stores the session and exposes the resolved role after a successful login', () => {
    let resolvedRole: string | undefined;
    service.login({ phone: '+237600000001', code: '123456' }).subscribe((response) => {
      resolvedRole = response.role;
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush({ token: fakeToken('BUREAU'), role: 'BUREAU', tenantId: 'tenant-bgft-douala' });

    expect(resolvedRole).toBe('BUREAU');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.role()).toBe('BUREAU');
    expect(service.session()?.tenantId).toBe('tenant-bgft-douala');
    expect(service.session()?.actorId).toBe('actor-1');
  });

  it('clears the session on logout', () => {
    service.login({ phone: '+237600000001', code: '123456' }).subscribe();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ token: fakeToken('ADMIN'), role: 'ADMIN', tenantId: 'tenant-flysoft' });

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem('fretcorridor.session')).toBeNull();
  });

  it('liste les tenants disponibles pour le compte connecté', () => {
    let tenants: { tenantId: string; origine: boolean }[] | undefined;
    service.mesTenants().subscribe((resultat) => {
      tenants = resultat;
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/tenants`);
    expect(req.request.method).toBe('GET');
    req.flush([
      { tenantId: 'tenant-bgft-douala', origine: true },
      { tenantId: 'tenant-bnft-ndjamena', origine: false },
    ]);

    expect(tenants).toEqual([
      { tenantId: 'tenant-bgft-douala', origine: true },
      { tenantId: 'tenant-bnft-ndjamena', origine: false },
    ]);
  });

  it('remplace la session par le JWT scopé au tenant choisi', () => {
    service.login({ phone: '+237600000002', code: '123456' }).subscribe();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/login`)
      .flush({ token: fakeToken('TRANSPORTEUR'), role: 'TRANSPORTEUR', tenantId: 'tenant-bgft-douala' });

    service.selectionnerTenant('tenant-bnft-ndjamena').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/tenants/selection`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ tenantId: 'tenant-bnft-ndjamena' });
    req.flush({
      token: fakeToken('TRANSPORTEUR'),
      role: 'TRANSPORTEUR',
      tenantId: 'tenant-bnft-ndjamena',
    });

    expect(service.session()?.tenantId).toBe('tenant-bnft-ndjamena');
  });
});
