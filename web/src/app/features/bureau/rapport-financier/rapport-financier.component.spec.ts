import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { RapportFinancierComponent } from './rapport-financier.component';
import { environment } from '../../../../environments/environment';

describe('RapportFinancierComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RapportFinancierComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche les ecritures du tenant au chargement', () => {
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
});
