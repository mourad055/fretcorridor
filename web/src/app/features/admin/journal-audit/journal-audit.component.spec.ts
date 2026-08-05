import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
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

  it('affiche les entrees du journal au chargement', () => {
    const fixture = TestBed.createComponent(JournalAuditComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`).flush([
      { id: 'e1', tenantId: 'tenant-bgft-douala', acteurId: 'actor-admin-1', action: 'DOSSIER_OUVERT', ressource: 'dossier:d1', horodatage: '2026-08-05T00:00:00Z' },
    ]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('DOSSIER_OUVERT');
  });

  it("affiche un message si le journal est vide", () => {
    const fixture = TestBed.createComponent(JournalAuditComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("Aucune entrée");
  });
});
