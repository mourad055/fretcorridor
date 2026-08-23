import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { axe } from 'jest-axe';
import { JournalAuditComponent } from './journal-audit.component';
import { environment } from '../../../../environments/environment';

describe('JournalAuditComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JournalAuditComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function flushTenants(fixture: ReturnType<typeof TestBed.createComponent>): void {
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([
      { id: 'tenant-bgft-douala', nom: 'BGFT Douala', pays: 'CM', actif: true },
    ]);
  }

  it('affiche les entrees du journal au chargement, tous tenants confondus par defaut', () => {
    const fixture = TestBed.createComponent(JournalAuditComponent);
    fixture.detectChanges();
    flushTenants(fixture);

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`);
    expect(req.request.params.has('tenantId')).toBe(false);
    req.flush([
      { id: 'e1', tenantId: 'tenant-bgft-douala', acteurId: 'actor-admin-1', action: 'DOSSIER_OUVERT', ressource: 'dossier:d1', horodatage: '2026-08-05T00:00:00Z' },
    ]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Dossier ouvert');
  });

  it("affiche un message si le journal est vide", () => {
    const fixture = TestBed.createComponent(JournalAuditComponent);
    fixture.detectChanges();
    flushTenants(fixture);

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("Aucune entrée");
  });

  it('recharge le journal filtre sur le tenant selectionne dans le menu', () => {
    const fixture = TestBed.createComponent(JournalAuditComponent);
    fixture.detectChanges();
    flushTenants(fixture);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`).flush([]);
    fixture.detectChanges();

    const select: HTMLSelectElement = fixture.nativeElement.querySelector('#journal-audit-tenant');
    select.value = 'tenant-bgft-douala';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const reqFiltre = httpMock.expectOne(
      (r) => r.url === `${environment.apiBaseUrl}/admin/journal-audit` && r.params.get('tenantId') === 'tenant-bgft-douala'
    );
    reqFiltre.flush([]);
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable (select de tenant inclus)", async () => {
    const fixture = TestBed.createComponent(JournalAuditComponent);
    fixture.detectChanges();
    flushTenants(fixture);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`).flush([
      { id: 'e1', tenantId: 'tenant-bgft-douala', acteurId: 'actor-admin-1', action: 'DOSSIER_OUVERT', ressource: 'dossier:d1', horodatage: '2026-08-05T00:00:00Z' },
    ]);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
