import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { RapportFinancierAdminComponent } from './rapport-financier-admin.component';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

describe('RapportFinancierAdminComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RapportFinancierAdminComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateServiceForTests()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('does not fetch anything until the admin clicks Consulter', () => {
    const fixture = TestBed.createComponent(RapportFinancierAdminComponent);
    fixture.detectChanges();

    httpMock.expectNone(`${environment.apiBaseUrl}/admin/rapport-financier/tenant-bgft-douala`);
    httpMock.expectNone(`${environment.apiBaseUrl}/admin/paiements-especes/tenant-bgft-douala`);
  });

  it('fetches the report of the selected tenant on demand', () => {
    const fixture = TestBed.createComponent(RapportFinancierAdminComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulter();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/rapport-financier/tenant-bgft-douala`).flush([
      { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 500, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: 'VIREMENT', litigeActif: false },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/paiements-especes/tenant-bgft-douala`).flush([]);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-ecritures-table'))).toBeTruthy();
  });

  it('calcule les totaux (credit, debit, solde) a partir des ecritures recues', () => {
    const fixture = TestBed.createComponent(RapportFinancierAdminComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulter();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/rapport-financier/tenant-bgft-douala`).flush([
      { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 500, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: 'VIREMENT', litigeActif: false },
      { id: 'e2', missionId: 'mission-1', typeCompte: 'COMPTE_TRANSPORTEUR', nature: 'REVERSEMENT', sens: 'DEBIT', montant: 300, creeLe: '2026-01-02T00:00:00Z', statut: 'VALIDE', modePaiement: null, litigeActif: false },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/paiements-especes/tenant-bgft-douala`).flush([]);
    fixture.detectChanges();

    expect(fixture.componentInstance.totaux()).toEqual({ nombre: 2, totalCredit: 500, totalDebit: 300, solde: 200 });
  });

  it('fetches the cash payments of the selected tenant on demand', () => {
    const fixture = TestBed.createComponent(RapportFinancierAdminComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulter();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/rapport-financier/tenant-bgft-douala`).flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/paiements-especes/tenant-bgft-douala`).flush([
      { id: 'd1', missionId: 'mission-especes-1', montant: 150, declareeLe: '2026-01-01T00:00:00Z', protectionAssuree: false },
    ]);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-especes-table'))).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('mission-especes-1');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(RapportFinancierAdminComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulter();
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/rapport-financier/tenant-bgft-douala`).flush([
      { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 500, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: 'VIREMENT', litigeActif: false },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/paiements-especes/tenant-bgft-douala`).flush([]);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
