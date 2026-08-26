import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { KycDashboardComponent } from './kyc-dashboard.component';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

const PENDING = [
  {
    id: 'kyc-1',
    acteurNom: 'Jean Mbarga',
    acteurTelephone: '+237677000001',
    typeActeur: 'CHAUFFEUR',
    soumisLe: '2026-01-01T00:00:00Z',
    statut: 'EN_ATTENTE',
    niveauKyc: 'NIVEAU_1',
  },
  {
    id: 'kyc-2',
    acteurNom: 'Transport Étoile SARL',
    acteurTelephone: '+237677000002',
    typeActeur: 'TRANSPORTEUR',
    soumisLe: '2026-01-02T00:00:00Z',
    statut: 'EN_ATTENTE',
    niveauKyc: 'NIVEAU_1',
  },
];

const N1 = [PENDING[0]];
const N2 = [
  {
    id: 'kyc-n2',
    acteurNom: 'Validé SARL',
    acteurTelephone: '+237677000099',
    typeActeur: 'TRANSPORTEUR',
    soumisLe: '2026-01-03T00:00:00Z',
    statut: 'VALIDE',
    niveauKyc: 'NIVEAU_2',
  },
];

function flushInitialLists(httpMock: HttpTestingController): void {
  httpMock.expectOne(`${environment.apiBaseUrl}/admin/kyc/pending`).flush(PENDING);
  httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/kyc` && r.params.get('niveau') === 'NIVEAU_1').flush(N1);
  httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/kyc` && r.params.get('niveau') === 'NIVEAU_2').flush(N2);
}

describe('KycDashboardComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KycDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateServiceForTests(),
        provideRouter([]),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    jest.spyOn(window, 'confirm').mockReturnValue(true);
  });

  afterEach(() => {
    httpMock.verify();
    jest.restoreAllMocks();
  });

  it('affiche la liste des dossiers en attente au chargement', () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();
    flushInitialLists(httpMock);
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain('Chauffeur');
    expect(fixture.nativeElement.textContent).toContain('2 dossier(s) en attente');
    expect(fixture.nativeElement.textContent).toContain('Validés N1');
  });

  it('filtre sur Validés N2', () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();
    flushInitialLists(httpMock);
    fixture.detectChanges();

    const kpis = fixture.debugElement.queryAll(By.css('.fc-kpi'));
    kpis[2].nativeElement.click();
    const req = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/kyc` && r.params.get('niveau') === 'NIVEAU_2');
    req.flush(N2);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Validé SARL');
    expect(fixture.debugElement.queryAll(By.css('tbody tr'))).toHaveLength(1);
  });

  it('ouvre le détail avec les pièces', () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();
    flushInitialLists(httpMock);
    fixture.detectChanges();

    const voir = fixture.debugElement.queryAll(By.css('button')).find((b) => b.nativeElement.textContent.includes('Voir détail'));
    voir!.nativeElement.click();
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/kyc/kyc-1`).flush({
      id: 'kyc-1',
      telephone: '+237677000001',
      nom: 'Mbarga',
      prenom: 'Jean',
      raisonSociale: null,
      niveauKyc: 'NIVEAU_1',
      roles: ['CHAUFFEUR'],
      pieces: [{ typeDocument: 'CNI', url: 'https://minio/cni', dateDepot: '2026-01-01T00:00:00' }],
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Pièces justificatives');
    expect(fixture.nativeElement.textContent).toContain('CNI');
  });

  it('retire un dossier de la liste apres validation', () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();
    flushInitialLists(httpMock);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.decide(PENDING[0] as never, 'VALIDE');

    const decisionReq = httpMock.expectOne(`${environment.apiBaseUrl}/admin/kyc/kyc-1/decision`);
    decisionReq.flush({ ...PENDING[0], statut: 'VALIDE', niveauKyc: 'NIVEAU_2' });

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/kyc/pending`).flush([PENDING[1]]);
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/kyc` && r.params.get('niveau') === 'NIVEAU_1').flush([]);
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/kyc` && r.params.get('niveau') === 'NIVEAU_2').flush([...N2, { ...PENDING[0], niveauKyc: 'NIVEAU_2' }]);
    fixture.detectChanges();

    expect(component.dossiers()).toHaveLength(1);
    expect(component.dossiers()[0].id).toBe('kyc-2');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();

    const pending = httpMock.expectOne(`${environment.apiBaseUrl}/admin/kyc/pending`);
    const n1 = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/kyc` && r.params.get('niveau') === 'NIVEAU_1');
    const n2 = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/kyc` && r.params.get('niveau') === 'NIVEAU_2');
    n1.flush([]);
    n2.flush([]);
    pending.flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert = fixture.debugElement.query(By.css('[role="alert"]'));
    expect(alert.nativeElement.textContent).toContain('Impossible de charger');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();
    flushInitialLists(httpMock);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
