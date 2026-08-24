import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NotificationAdminService } from './notification-admin.service';
import { environment } from '../../../../environments/environment';

describe('NotificationAdminService', () => {
  let service: NotificationAdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(NotificationAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('remonte les dossiers en retard (delai depasse, pas clos) de tous les tenants', () => {
    let result: unknown;
    service.dossiersEnRetard().subscribe((r) => (result = r));

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([
      { id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true },
      { id: 'tenant-bnft-ndjamena', nom: "Bureau N'Djamena", pays: 'Tchad', actif: true },
    ]);

    httpMock
      .expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers` && r.params.get('tenantId') === 'tenant-bgft-douala')
      .flush([
        { id: 'd1', tenantId: 'tenant-bgft-douala', type: 'LITIGE', priorite: 'HAUTE', statut: 'OUVERT', missionId: 'm1', parties: [], preuvesReferences: [], ouvertLe: '2020-01-01T00:00:00Z', delaiTraitement: '2020-01-02T00:00:00Z', priseEnChargeParActeurId: null, decision: null, motifDecision: null, decidePar: null, decideLe: null },
        { id: 'd2', tenantId: 'tenant-bgft-douala', type: 'INCIDENT', priorite: 'NORMALE', statut: 'CLOS', missionId: 'm2', parties: [], preuvesReferences: [], ouvertLe: '2020-01-01T00:00:00Z', delaiTraitement: '2020-01-02T00:00:00Z', priseEnChargeParActeurId: null, decision: null, motifDecision: null, decidePar: null, decideLe: null },
      ]);
    httpMock
      .expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers` && r.params.get('tenantId') === 'tenant-bnft-ndjamena')
      .flush([]);

    expect(result).toEqual([
      {
        titre: 'Dossier en retard — Bureau Douala',
        detail: 'LITIGE (HAUTE) — délai dépassé le 2020-01-02T00:00:00Z',
        tenantId: 'tenant-bgft-douala',
        dossierId: 'd1',
      },
    ]);
  });

  it('traite un tenant dont la file de travail echoue comme sans dossier (pas de blocage global)', () => {
    let result: unknown;
    service.dossiersEnRetard().subscribe((r) => (result = r));

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([
      { id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true },
    ]);
    httpMock
      .expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });

    expect(result).toEqual([]);
  });
});
