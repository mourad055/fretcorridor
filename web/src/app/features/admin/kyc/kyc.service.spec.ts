import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { KycService } from './kyc.service';
import { environment } from '../../../../environments/environment';
import { TENANT_ADMIN_DEFAUT } from '../admin-tenants';

describe('KycService', () => {
  let service: KycService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(KycService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the pending dossiers', () => {
    let result: unknown;
    service.listPending(TENANT_ADMIN_DEFAUT).subscribe((dossiers) => (result = dossiers));

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc/pending` &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    );
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'kyc-1', acteurNom: 'Jean', acteurTelephone: '+237600', typeActeur: 'CHAUFFEUR', soumisLe: '2026-01-01T00:00:00Z', statut: 'EN_ATTENTE' }]);

    expect(result).toHaveLength(1);
  });

  it('lists dossiers by niveau', () => {
    service.listByNiveau(TENANT_ADMIN_DEFAUT, 'NIVEAU_2').subscribe();
    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc` &&
        r.params.get('niveau') === 'NIVEAU_2' &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('loads dossier detail', () => {
    service.detail(TENANT_ADMIN_DEFAUT, 'kyc-1').subscribe();
    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc/kyc-1` &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'kyc-1',
      telephone: '+237600',
      nom: 'Jean',
      prenom: null,
      raisonSociale: null,
      niveauKyc: 'NIVEAU_1',
      roles: ['CHAUFFEUR'],
      pieces: [],
    });
  });

  it('sends a decision with an idempotency key header', () => {
    service.decide(TENANT_ADMIN_DEFAUT, 'kyc-1', 'VALIDE').subscribe();

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/admin/kyc/kyc-1/decision` &&
        r.params.get('tenantId') === TENANT_ADMIN_DEFAUT
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ decision: 'VALIDE', motif: null });
    expect(req.request.headers.has('X-Idempotency-Key')).toBe(true);
    req.flush({ id: 'kyc-1', acteurNom: 'Jean', acteurTelephone: '+237600', typeActeur: 'CHAUFFEUR', soumisLe: '2026-01-01T00:00:00Z', statut: 'VALIDE' });
  });
});
