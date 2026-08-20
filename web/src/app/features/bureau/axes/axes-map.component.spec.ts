import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { AxesMapComponent } from './axes-map.component';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

describe('AxesMapComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AxesMapComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateServiceForTests()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche un axe par ligne du tableau au chargement', () => {
    const fixture = TestBed.createComponent(AxesMapComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/axes`).flush([
      { id: 'axe-1', origine: 'Douala', destination: 'Yaoundé', distanceKm: 300, visibiliteActive: true, matchingActif: true, paiementActif: true },
      { id: 'axe-2', origine: 'Douala', destination: 'Bafoussam', distanceKm: 350, visibiliteActive: true, matchingActif: true, paiementActif: false },
    ]);
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('table.axes-table tbody tr'));
    expect(rows).toHaveLength(2);
    expect(fixture.debugElement.query(By.css('app-corridor-map'))).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Matching');
  });

  it('sélectionne une ligne au clic puis la désélectionne au second clic', () => {
    const fixture = TestBed.createComponent(AxesMapComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/axes`).flush([
      { id: 'axe-1', origine: 'Douala', destination: 'Yaoundé', distanceKm: 300, visibiliteActive: true, matchingActif: true, paiementActif: true },
      { id: 'axe-2', origine: 'Douala', destination: 'Bafoussam', distanceKm: 350, visibiliteActive: true, matchingActif: true, paiementActif: false },
    ]);
    fixture.detectChanges();

    const [premiereLigne, deuxiemeLigne] = fixture.debugElement.queryAll(By.css('table.axes-table tbody tr'));

    premiereLigne.nativeElement.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.axeSelectionneId()).toBe('axe-1');
    expect(premiereLigne.nativeElement.classList).toContain('axes-table__row--selected');
    expect(premiereLigne.nativeElement.getAttribute('aria-selected')).toBe('true');
    expect(deuxiemeLigne.nativeElement.getAttribute('aria-selected')).toBe('false');

    premiereLigne.nativeElement.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.axeSelectionneId()).toBeNull();
    expect(premiereLigne.nativeElement.classList).not.toContain('axes-table__row--selected');
  });

  it("affiche un message si aucun axe n'est activé", () => {
    const fixture = TestBed.createComponent(AxesMapComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/axes`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun axe activé');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(AxesMapComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/bureau/axes`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });
});
