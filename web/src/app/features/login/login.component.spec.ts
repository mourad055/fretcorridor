import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { LoginComponent } from './login.component';
import { environment } from '../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../testing/translate-testing.providers';

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

  it('redirects to the role home route on successful login', async () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const navigateSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const component = fixture.componentInstance;
    component.phone.set('+237600000001');
    component.code.set('123456');
    component.submit();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    req.flush({ token: 'header.eyJzdWIiOiJhIn0.sig', role: 'BUREAU', tenantId: 'tenant-1' });

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

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    req.flush({ token: 'header.eyJzdWIiOiJhIn0.sig', role: 'BUREAU', tenantId: 'tenant-1' });

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

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    req.flush({ token: 'header.eyJzdWIiOiJiIn0.sig', role: 'BUREAU', tenantId: 'tenant-bnft-ndjamena' });

    expect(navigateSpy).toHaveBeenCalledWith('/bureau');
  });

  it('affiche le sélecteur de langue (comportement testé dans LangueSwitchComponent)', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-langue-switch'))).toBeTruthy();
  });
});
