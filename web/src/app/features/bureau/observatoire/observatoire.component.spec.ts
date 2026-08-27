import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { axe } from 'jest-axe';
import { ObservatoireComponent } from './observatoire.component';
import { environment } from '../../../../environments/environment';

describe('ObservatoireComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ObservatoireComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function flushAxesEtObservatoire(): void {
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/axes`).flush([
      { id: 'axe-1', origine: 'Douala', destination: 'Yaoundé', distanceKm: 300, visibiliteActive: true, matchingActif: true, paiementActif: true },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/observatoire/axe-1`).flush({
      axeId: 'axe-1',
      seuil: 5,
      seuilAtteint: false,
      nombreMissions: null,
      prixMediane: null,
      prixDispersion: null,
      devise: null,
      tauxDesequilibreDirectionnel: null,
      couverturePourcentage: null,
      estimationDefinieLe: null,
    });
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/alertes/etat`).flush([]);
  }

  it("charge le premier axe et affiche le message d'anonymisation quand le seuil n'est pas atteint", () => {
    const fixture = TestBed.createComponent(ObservatoireComponent);
    fixture.detectChanges();
    flushAxesEtObservatoire();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("Seuil d'agrégation non atteint");
  });

  it('affiche les indicateurs quand le seuil est atteint', () => {
    const fixture = TestBed.createComponent(ObservatoireComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/axes`).flush([
      { id: 'axe-1', origine: 'Douala', destination: 'Yaoundé', distanceKm: 300, visibiliteActive: true, matchingActif: true, paiementActif: true },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/observatoire/axe-1`).flush({
      axeId: 'axe-1',
      seuil: 5,
      seuilAtteint: true,
      nombreMissions: 12,
      prixMediane: 45000,
      prixDispersion: 3000,
      devise: 'XAF',
      tauxDesequilibreDirectionnel: 0.2,
      couverturePourcentage: null,
      estimationDefinieLe: null,
    });
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/alertes/etat`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('12');
    expect(fixture.nativeElement.textContent).toContain('45000');
  });

  it('efface une erreur obsolète quand un autre axe se charge correctement', () => {
    const fixture = TestBed.createComponent(ObservatoireComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/axes`).flush([
      { id: 'axe-1', origine: 'Douala', destination: 'Yaoundé', distanceKm: 300, visibiliteActive: true, matchingActif: true, paiementActif: true },
      { id: 'axe-2', origine: 'Yaoundé', destination: "N'Djamena", distanceKm: 1200, visibiliteActive: true, matchingActif: true, paiementActif: true },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/observatoire/axe-1`).error(new ProgressEvent('error'));
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/alertes/etat`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("Impossible de charger l'observatoire de cet axe.");

    fixture.componentInstance.onAxeChange('axe-2');
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/observatoire/axe-2`).flush({
      axeId: 'axe-2',
      seuil: 3,
      seuilAtteint: true,
      nombreMissions: 4,
      prixMediane: 420000,
      prixDispersion: 35000,
      devise: 'XAF',
      tauxDesequilibreDirectionnel: 0.75,
      couverturePourcentage: null,
      estimationDefinieLe: null,
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain("Impossible de charger l'observatoire de cet axe.");
    expect(fixture.nativeElement.textContent).not.toContain("Seuil d'agrégation non atteint");
    expect(fixture.nativeElement.textContent).toContain('420000');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(ObservatoireComponent);
    fixture.detectChanges();
    flushAxesEtObservatoire();
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
