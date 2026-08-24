import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { PositionsListComponent } from './positions-list.component';
import { environment } from '../../../../environments/environment';

const POSITIONS = [
  {
    id: 'pos-1',
    vehiculeLabel: 'Camion 10T — LT 1234 AB',
    latitude: 4.0511,
    longitude: 9.7679,
    capturedLe: '2026-01-01T00:00:00Z',
    ageSecondes: 90,
  },
];

describe('PositionsListComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PositionsListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it("affiche l'age de chaque position au chargement, jamais l'horodatage seul", () => {
    const fixture = TestBed.createComponent(PositionsListComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/positions`).flush(POSITIONS);
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('il y a 1 min');
  });

  it("affiche un message si aucun vehicule n'est en suivi", () => {
    const fixture = TestBed.createComponent(PositionsListComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/positions`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun véhicule en suivi');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(PositionsListComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/bureau/positions`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(PositionsListComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/positions`).flush(POSITIONS);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
