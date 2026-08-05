import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { CapacitesListComponent } from './capacites-list.component';
import { environment } from '../../../../environments/environment';

const CAPACITES = [
  {
    id: 'cap-1',
    vehicule: 'Camion 10T',
    origine: 'Douala',
    destination: 'Yaoundé',
    departLe: '2026-01-01T00:00:00Z',
    poidsTaxableKg: 9500,
    modeCollecte: 'PORTE_A_PORTE',
    etat: 'PUBLIEE',
  },
];

describe('CapacitesListComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CapacitesListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  /** Le composant embarque <app-transporteur-missions> et <app-paiement>, qui déclenchent leurs propres requêtes au chargement. */
  function flushMissionsRequest(): void {
    httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/missions`).flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/paiement`).flush({ solde: 0, historique: [] });
  }

  it('affiche une ligne par capacite declaree au chargement', () => {
    const fixture = TestBed.createComponent(CapacitesListComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/capacites`).flush(CAPACITES);
    flushMissionsRequest();
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows).toHaveLength(1);
  });

  it("affiche un message si aucune capacite n'est declaree", () => {
    const fixture = TestBed.createComponent(CapacitesListComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/capacites`).flush([]);
    flushMissionsRequest();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune capacité déclarée');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(CapacitesListComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/transporteur/capacites`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    flushMissionsRequest();
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });
});
