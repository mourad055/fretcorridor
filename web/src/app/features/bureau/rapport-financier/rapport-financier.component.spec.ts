import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { RapportFinancierComponent } from './rapport-financier.component';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

function activatedRouteAvec(queryParams: Record<string, string>) {
  const paramMap = convertToParamMap(queryParams);
  return {
    snapshot: { queryParamMap: paramMap },
    queryParamMap: of(paramMap),
  };
}

describe('RapportFinancierComponent', () => {
  let httpMock: HttpTestingController;

  function configure(queryParams: Record<string, string> = {}): void {
    TestBed.configureTestingModule({
      imports: [RapportFinancierComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateServiceForTests(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: activatedRouteAvec(queryParams) },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  }

  afterEach(() => httpMock.verify());

  it('affiche les ecritures du tenant au chargement', () => {
    configure();
    const fixture = TestBed.createComponent(RapportFinancierComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/rapport-financier`).flush([
      { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 500, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: 'VIREMENT', litigeActif: false },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/paiements-especes`).flush([]);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-ecritures-table'))).toBeTruthy();
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    configure();
    const fixture = TestBed.createComponent(RapportFinancierComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/bureau/rapport-financier`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/paiements-especes`).flush([]);
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });

  it('affiche les paiements en espèces du territoire', () => {
    configure();
    const fixture = TestBed.createComponent(RapportFinancierComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/rapport-financier`).flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/paiements-especes`).flush([
      { id: 'd1', missionId: 'mission-especes-1', montant: 150, declareeLe: '2026-01-01T00:00:00Z', protectionAssuree: false },
    ]);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-especes-table'))).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('mission-especes-1');
  });

  it('filtre les ecritures affichees sur le missionId passe en query param (drill-down depuis Missions)', () => {
    configure({ missionId: 'mission-1' });
    const fixture = TestBed.createComponent(RapportFinancierComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/rapport-financier`).flush([
      { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 500, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: 'VIREMENT', litigeActif: false },
      { id: 'e2', missionId: 'mission-2', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 300, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: 'VIREMENT', litigeActif: false },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/paiements-especes`).flush([]);
    fixture.detectChanges();

    expect(fixture.componentInstance.ecrituresAffichees()).toHaveLength(1);
    expect(fixture.componentInstance.totaux().totalCredit).toBe(500);
    expect(fixture.nativeElement.textContent).toContain('Filtré sur la mission mission-1');
  });
});
