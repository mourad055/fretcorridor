import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { BureauChronologieComponent } from './bureau-chronologie.component';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

const MISSIONS = [
  {
    id: 'mission-a',
    transporteurNom: 'Transport Étoile SARL',
    origine: 'Douala',
    destination: 'Yaoundé',
    etapes: [{ rang: 1, type: 'ENLEVEMENT', lieu: 'Douala', etat: 'TERMINEE' }],
  },
];

describe('BureauChronologieComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BureauChronologieComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateServiceForTests()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche la chronologie des missions du territoire au chargement', () => {
    const fixture = TestBed.createComponent(BureauChronologieComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-chronologie`).flush(MISSIONS);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-mission-chronologie'))).toBeTruthy();
  });

  it("affiche un message si aucune mission n'existe sur le territoire", () => {
    const fixture = TestBed.createComponent(BureauChronologieComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-chronologie`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune mission sur votre territoire');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(BureauChronologieComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/bureau/missions-chronologie`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });
});
