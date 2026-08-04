import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { TransporteurMissionsComponent } from './transporteur-missions.component';
import { environment } from '../../../../environments/environment';

const MISSIONS = [
  {
    id: 'mission-a',
    transporteurNom: 'Transport Étoile SARL',
    origine: 'Douala',
    destination: 'Yaoundé',
    etapes: [{ rang: 1, type: 'ENLEVEMENT', lieu: 'Douala', etat: 'TERMINEE' }],
  },
];

describe('TransporteurMissionsComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransporteurMissionsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche la chronologie de ses propres missions au chargement', () => {
    const fixture = TestBed.createComponent(TransporteurMissionsComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/missions`).flush(MISSIONS);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-mission-chronologie'))).toBeTruthy();
  });

  it("affiche un message si aucune mission n'existe", () => {
    const fixture = TestBed.createComponent(TransporteurMissionsComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/transporteur/missions`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune mission pour l');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(TransporteurMissionsComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/transporteur/missions`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });
});
