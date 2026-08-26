import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { LoginComponent } from './login.component';
import { environment } from '../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../testing/translate-testing.providers';
import { TenantOption } from '../../core/auth/auth.models';

const TENANT_ORIGINE: TenantOption = { tenantId: 'tenant-1', origine: true };

describe('LoginComponent', () => {
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'bureau', children: [] }]),
        provideTranslateServiceForTests(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  function flushLogin(body: { token: string; role: string; tenantId: string }): void {
    httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`).flush(body);
  }

  function flushTenants(tenants: TenantOption[]): void {
    httpMock.expectOne(`${environment.apiBaseUrl}/auth/tenants`).flush(tenants);
  }

  it('redirects to the role home route on successful login', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const component = fixture.componentInstance;
    component.phone.set('+237600000001');
    component.code.set('123456');
    component.submit();

    flushLogin({ token: 'header.eyJzdWIiOiJhIn0.sig', role: 'BUREAU', tenantId: 'tenant-1' });
    flushTenants([TENANT_ORIGINE]);

    expect(navigateSpy).toHaveBeenCalledWith('/bureau');
  });

  it('shows an error message on invalid credentials, without navigating', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const navigateSpy = jest.spyOn(router, 'navigateByUrl');

    const component = fixture.componentInstance;
    component.phone.set('+237699999999');
    component.code.set('000000');
    component.submit();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    req.flush({ title: 'Authentification refusée' }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(component.errorMessage()).toBe('login.error');
    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Numéro de téléphone ou code invalide.');
  });

  it('logs in with the demo account credentials and redirects to its home route', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const component = fixture.componentInstance;
    component.loginAsDemo(component.demoAccounts[0]);

    expect(component.phone()).toBe('+237600000001');
    expect(component.code()).toBe('1234');

    flushLogin({ token: 'header.eyJzdWIiOiJhIn0.sig', role: 'BUREAU', tenantId: 'tenant-1' });
    flushTenants([TENANT_ORIGINE]);

    expect(navigateSpy).toHaveBeenCalledWith('/bureau');
  });

  it('logs in with the Chad bureau demo account and redirects to its home route', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const component = fixture.componentInstance;
    const compteTchad = component.demoAccounts.find((account) => account.phone === '+235600000004');
    component.loginAsDemo(compteTchad!);

    expect(component.phone()).toBe('+235600000004');
    expect(component.code()).toBe('1234');

    flushLogin({ token: 'header.eyJzdWIiOiJiIn0.sig', role: 'BUREAU', tenantId: 'tenant-bnft-ndjamena' });
    flushTenants([{ tenantId: 'tenant-bnft-ndjamena', origine: true }]);

    expect(navigateSpy).toHaveBeenCalledWith('/bureau');
  });

  it("entre dans l'app meme si la liste des tenants est indisponible (ENF-DIS-04)", () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const component = fixture.componentInstance;
    component.phone.set('+237600000001');
    component.code.set('123456');
    component.submit();

    flushLogin({ token: 'header.eyJzdWIiOiJhIn0.sig', role: 'BUREAU', tenantId: 'tenant-1' });
    httpMock
      .expectOne(`${environment.apiBaseUrl}/auth/tenants`)
      .flush({ detail: 'Service indisponible' }, { status: 503, statusText: 'Service Unavailable' });

    expect(navigateSpy).toHaveBeenCalledWith('/bureau');
    expect(component.tenantsAChoisir()).toBeNull();
  });

  it("propose un choix si le compte est rattaché à plusieurs bureaux, sans naviguer", () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const component = fixture.componentInstance;
    component.phone.set('+237600000002');
    component.code.set('123456');
    component.submit();

    flushLogin({ token: 'header.eyJzdWIiOiJhIn0.sig', role: 'TRANSPORTEUR', tenantId: 'tenant-1' });
    flushTenants([
      { tenantId: 'tenant-bgft-douala', origine: true },
      { tenantId: 'tenant-bnft-ndjamena', origine: false },
    ]);
    fixture.detectChanges();

    expect(navigateSpy).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Choisir un bureau');
    expect(fixture.nativeElement.textContent).toContain("BNFT N'Djamena");
    expect(fixture.nativeElement.textContent).toContain('BGFT Douala');
    expect(fixture.nativeElement.textContent).toContain('Bureau principal');
  });

  it('remplace le JWT puis navigue après le choix d\'un tenant', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const component = fixture.componentInstance;
    component.phone.set('+237600000002');
    component.code.set('123456');
    component.submit();

    flushLogin({ token: 'header.eyJzdWIiOiJhIn0.sig', role: 'TRANSPORTEUR', tenantId: 'tenant-1' });
    flushTenants([
      { tenantId: 'tenant-bgft-douala', origine: true },
      { tenantId: 'tenant-bnft-ndjamena', origine: false },
    ]);
    fixture.detectChanges();

    component.choisirTenant('tenant-bnft-ndjamena');
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/tenants/selection`);
    expect(req.request.body).toEqual({ tenantId: 'tenant-bnft-ndjamena' });
    req.flush({ token: 'header.eyJzdWIiOiJhIn0.sig', role: 'TRANSPORTEUR', tenantId: 'tenant-bnft-ndjamena' });

    expect(navigateSpy).toHaveBeenCalledWith('/transporteur');
  });

  it('affiche le sélecteur de langue (comportement testé dans LangueSwitchComponent)', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-langue-switch'))).toBeTruthy();
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });

  it("n'a aucune violation d'accessibilité sur l'écran de choix de tenant", async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.phone.set('+237600000002');
    component.code.set('123456');
    component.submit();

    flushLogin({ token: 'header.eyJzdWIiOiJhIn0.sig', role: 'TRANSPORTEUR', tenantId: 'tenant-1' });
    flushTenants([
      { tenantId: 'tenant-bgft-douala', origine: true },
      { tenantId: 'tenant-bnft-ndjamena', origine: false },
    ]);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
