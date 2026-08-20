import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { KycDashboardComponent } from './kyc-dashboard.component';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

const PENDING = [
  { id: 'kyc-1', acteurNom: 'Jean Mbarga', acteurTelephone: '+237677000001', typeActeur: 'CHAUFFEUR', soumisLe: '2026-01-01T00:00:00Z', statut: 'EN_ATTENTE' },
  { id: 'kyc-2', acteurNom: 'Transport Étoile SARL', acteurTelephone: '+237677000002', typeActeur: 'TRANSPORTEUR_PERSONNE_MORALE', soumisLe: '2026-01-02T00:00:00Z', statut: 'EN_ATTENTE' },
];

describe('KycDashboardComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KycDashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateServiceForTests()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche la liste des dossiers en attente au chargement', () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/kyc/pending`).flush(PENDING);
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain('Chauffeur');
    expect(fixture.nativeElement.textContent).toContain('Transporteur (personne morale)');
  });

  it("retire un dossier de la liste apres validation", () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/kyc/pending`).flush(PENDING);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.decide(PENDING[0] as never, 'VALIDE');

    const decisionReq = httpMock.expectOne(`${environment.apiBaseUrl}/admin/kyc/kyc-1/decision`);
    decisionReq.flush({ ...PENDING[0], statut: 'VALIDE' });
    fixture.detectChanges();

    expect(component.dossiers()).toHaveLength(1);
    expect(component.dossiers()[0].id).toBe('kyc-2');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/kyc/pending`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });
});
