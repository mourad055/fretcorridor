import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { MissionsListComponent } from './missions-list.component';
import { environment } from '../../../../environments/environment';

const MISSIONS = [
  {
    id: 'mission-1',
    transporteurNom: 'Transport Étoile SARL',
    origine: 'Douala',
    destination: 'Yaoundé',
    enlevementLe: '2026-01-01T00:00:00Z',
    statut: 'CONFIRMEE',
  },
];

describe('MissionsListComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MissionsListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche une ligne par mission appariee au chargement', () => {
    const fixture = TestBed.createComponent(MissionsListComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees`).flush(MISSIONS);
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows).toHaveLength(1);
  });

  it("affiche un message si aucune mission n'est appariee", () => {
    const fixture = TestBed.createComponent(MissionsListComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune mission appariée');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(MissionsListComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });
});
