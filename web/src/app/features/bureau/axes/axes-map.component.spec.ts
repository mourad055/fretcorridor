import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { AxesMapComponent } from './axes-map.component';
import { environment } from '../../../../environments/environment';

describe('AxesMapComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AxesMapComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  /** Le composant embarque missions-list, positions-list, bureau-chronologie, rapport-financier et notifications, qui déclenchent leurs propres requêtes au chargement. */
  function flushSiblingRequests(): void {
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees`).flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/positions`).flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-chronologie`).flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/rapport-financier`).flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/notifications`).flush([]);
  }

  it('affiche un axe par ligne du tableau au chargement', () => {
    const fixture = TestBed.createComponent(AxesMapComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/axes`).flush([
      { id: 'axe-1', origine: 'Douala', destination: 'Yaoundé', distanceKm: 300, etatActivation: 'PAIEMENT' },
      { id: 'axe-2', origine: 'Douala', destination: 'Bafoussam', distanceKm: 350, etatActivation: 'MATCHING' },
    ]);
    flushSiblingRequests();
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('table.axes-table tbody tr'));
    expect(rows).toHaveLength(2);
    expect(fixture.debugElement.query(By.css('app-corridor-map'))).toBeTruthy();
  });

  it("affiche un message si aucun axe n'est activé", () => {
    const fixture = TestBed.createComponent(AxesMapComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/axes`).flush([]);
    flushSiblingRequests();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun axe activé');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(AxesMapComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/bureau/axes`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    flushSiblingRequests();
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });
});
