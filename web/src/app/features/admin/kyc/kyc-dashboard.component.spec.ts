import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { KycDashboardComponent } from './kyc-dashboard.component';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';
import { TENANT_ADMIN_DEFAUT } from '../admin-tenants';

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
  httpMock
    .expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc/pending` &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    )
    .flush(PENDING);
  httpMock
    .expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc` &&
        r.params.get('niveau') === 'NIVEAU_1' &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    )
    .flush(N1);
  httpMock
    .expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc` &&
        r.params.get('niveau') === 'NIVEAU_2' &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    )
    .flush(N2);
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
  });

  afterEach(() => {
    httpMock.verify();
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
    expect(fixture.nativeElement.textContent).toContain('Profils N1 (mobile)');
  });

  it('filtre sur Validés admin (N2)', () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();
    flushInitialLists(httpMock);
    fixture.detectChanges();

    const kpis = fixture.debugElement.queryAll(By.css('.fc-kpi'));
    kpis[2].nativeElement.click();
    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc` &&
        r.params.get('niveau') === 'NIVEAU_2' &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    );
    req.flush(N2);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Validé SARL');
    expect(fixture.debugElement.queryAll(By.css('tbody tr'))).toHaveLength(1);
  });

  it('ouvre la CNI au clic sur la pièce', () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();
    flushInitialLists(httpMock);
    fixture.detectChanges();

    const voir = fixture.debugElement.queryAll(By.css('button')).find((b) => b.nativeElement.textContent.includes('Voir détail'));
    voir!.nativeElement.click();
    httpMock
      .expectOne(
        (r) =>
          r.url === `${environment.apiBaseUrl}/admin/kyc/kyc-1` &&
          r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
      )
      .flush({
        id: 'kyc-1',
        telephone: '+237677000001',
        nom: 'Mbarga',
        prenom: 'Jean',
        raisonSociale: null,
        niveauKyc: 'NIVEAU_1',
        roles: ['CHAUFFEUR'],
        pieces: [{ id: 'piece-1', typeDocument: 'CNI', url: 'https://minio/cni.jpg', dateDepot: '2026-01-01T00:00:00' }],
      });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Pièces justificatives');
    expect(fixture.nativeElement.textContent).toContain('Cliquer pour afficher la pièce');

    const ouvrirPiece = fixture.debugElement.query(By.css('.kyc-piece__open'));
    ouvrirPiece.nativeElement.click();
    const contentReq = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc/kyc-1/pieces/piece-1/content` &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    );
    contentReq.flush(new Blob(['fake'], { type: 'image/jpeg' }));
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.fc-modal.fc-modal--lg'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('.fc-modal__media'))).toBeTruthy();
  });

  it('retire un dossier de la liste apres validation', () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();
    flushInitialLists(httpMock);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.demanderDecision(PENDING[0] as never, 'VALIDE');
    fixture.detectChanges();
    const confirmer = fixture.debugElement.queryAll(By.css('.fc-modal__footer button')).find((b) =>
      b.nativeElement.textContent.includes('Valider')
    );
    confirmer!.nativeElement.click();

    const decisionReq = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc/kyc-1/decision` &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    );
    decisionReq.flush({ ...PENDING[0], statut: 'VALIDE', niveauKyc: 'NIVEAU_2' });

    httpMock
      .expectOne(
        (r) =>
          r.url === `${environment.apiBaseUrl}/admin/kyc/pending` &&
          r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
      )
      .flush([PENDING[1]]);
    httpMock
      .expectOne(
        (r) =>
          r.url === `${environment.apiBaseUrl}/admin/kyc` &&
          r.params.get('niveau') === 'NIVEAU_1' &&
          r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
      )
      .flush([]);
    httpMock
      .expectOne(
        (r) =>
          r.url === `${environment.apiBaseUrl}/admin/kyc` &&
          r.params.get('niveau') === 'NIVEAU_2' &&
          r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
      )
      .flush([...N2, { ...PENDING[0], niveauKyc: 'NIVEAU_2' }]);
    fixture.detectChanges();

    expect(component.dossiers()).toHaveLength(1);
    expect(component.dossiers()[0].id).toBe('kyc-2');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(KycDashboardComponent);
    fixture.detectChanges();

    const pending = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc/pending` &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    );
    const n1 = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc` &&
        r.params.get('niveau') === 'NIVEAU_1' &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    );
    const n2 = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc` &&
        r.params.get('niveau') === 'NIVEAU_2' &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    );
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
