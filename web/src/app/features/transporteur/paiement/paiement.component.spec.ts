import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { PaiementComponent } from './paiement.component';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

describe('PaiementComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaiementComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateServiceForTests()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche le solde et l historique au chargement', () => {
    const fixture = TestBed.createComponent(PaiementComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/paiement`).flush({
      solde: 150,
      historique: [
        { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_TRANSPORTEUR', nature: 'REVERSEMENT', sens: 'DEBIT', montant: 150, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE' },
      ],
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('150');
    expect(fixture.debugElement.query(By.css('app-ecritures-table'))).toBeTruthy();
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(PaiementComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/transporteur/paiement`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(PaiementComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/paiement`).flush({
      solde: 150,
      historique: [
        { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_TRANSPORTEUR', nature: 'REVERSEMENT', sens: 'DEBIT', montant: 150, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE', modePaiement: null, litigeActif: false },
      ],
    });
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
