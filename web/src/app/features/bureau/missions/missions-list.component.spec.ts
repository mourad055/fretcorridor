import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { MissionsListComponent } from './missions-list.component';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

const MISSIONS = [
  {
    id: 'mission-1',
    axeId: 'axe-1',
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
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateServiceForTests(), provideRouter([])],
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
    expect(fixture.nativeElement.textContent).toContain('Confirmée');
    expect(fixture.nativeElement.textContent).not.toContain('CONFIRMEE');
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

  it('recharge avec les paramètres de filtre quand on clique sur Filtrer', () => {
    const fixture = TestBed.createComponent(MissionsListComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees`).flush(MISSIONS);
    fixture.detectChanges();

    fixture.componentInstance.statutFiltre.set('EN_COURS');
    fixture.componentInstance.axeIdFiltre.set('axe-2');
    fixture.componentInstance.filtrer();

    const requete = httpMock.expectOne(
      (req) => req.url === `${environment.apiBaseUrl}/bureau/missions-appariees` && req.params.get('statut') === 'EN_COURS' && req.params.get('axeId') === 'axe-2',
    );
    requete.flush([]);
  });

  it('affiche le détail d\'une mission sélectionnée', () => {
    const fixture = TestBed.createComponent(MissionsListComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees`).flush(MISSIONS);
    fixture.detectChanges();

    fixture.debugElement.query(By.css('tbody tr button')).nativeElement.click();
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees/mission-1`).flush(MISSIONS[0]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Mission mission-1');
    expect(fixture.nativeElement.textContent).toContain('Douala → Yaoundé');

    const lienEcritures: HTMLAnchorElement = fixture.debugElement.query(By.css('.mission-detail a')).nativeElement;
    expect(lienEcritures.getAttribute('href')).toContain('/bureau/rapport-financier');
    expect(lienEcritures.getAttribute('href')).toContain('missionId=mission-1');
    expect(lienEcritures.textContent).toContain('Voir les écritures');
  });

  it('déclenche le téléchargement du CSV export en cliquant sur Exporter', () => {
    const fixture = TestBed.createComponent(MissionsListComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees`).flush(MISSIONS);
    fixture.detectChanges();

    fixture.componentInstance.exporter();

    const requete = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees/export`);
    expect(requete.request.responseType).toBe('text');
    requete.flush('id,axeId,transporteurNom,origine,destination,enlevementLe,statut\n');
    expect(fixture.componentInstance.exportEnCours()).toBe(false);
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(MissionsListComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/missions-appariees`).flush(MISSIONS);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
