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

  it('affiche une ligne par capacite declaree au chargement', () => {
    const fixture = TestBed.createComponent(CapacitesListComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/capacites`).flush(CAPACITES);
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('Porte à porte');
    expect(fixture.nativeElement.textContent).toContain('Publiée');
    expect(fixture.nativeElement.textContent).not.toContain('PORTE_A_PORTE');
  });

  it("affiche un message si aucune capacite n'est declaree", () => {
    const fixture = TestBed.createComponent(CapacitesListComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/capacites`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune capacité déclarée');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(CapacitesListComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/transporteur/capacites`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });
});
