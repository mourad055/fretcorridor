import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RechercheService } from './recherche.service';
import { environment } from '../../../../environments/environment';

describe('RechercheService', () => {
  let service: RechercheService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RechercheService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('combine les tenants et le journal d\'audit correspondant au terme recherche', () => {
    let result: unknown;
    service.rechercher('douala').subscribe((r) => (result = r));

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([
      { id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true },
      { id: 'tenant-bnft-ndjamena', nom: 'Bureau N\'Djamena', pays: 'Tchad', actif: true },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`).flush([
      { id: 'e1', tenantId: 'tenant-bgft-douala', acteurId: 'actor-admin-1', action: 'TENANT_CREE', ressource: 'tenant:tenant-bgft-douala', horodatage: '2026-08-01T00:00:00Z' },
      { id: 'e2', tenantId: 'tenant-flysoft', acteurId: 'actor-admin-2', action: 'DOSSIER_OUVERT', ressource: 'dossier:d1', horodatage: '2026-08-02T00:00:00Z' },
    ]);

    expect(result).toEqual([
      { type: 'TENANT', titre: 'Bureau Douala', detail: 'tenant-bgft-douala — Cameroun — actif', tenantId: 'tenant-bgft-douala' },
      { type: 'JOURNAL_AUDIT', titre: 'TENANT_CREE', detail: 'tenant:tenant-bgft-douala — actor-admin-1 — 2026-08-01T00:00:00Z', tenantId: 'tenant-bgft-douala' },
    ]);
  });

  it('ne renvoie rien si aucun element ne correspond', () => {
    let result: unknown;
    service.rechercher('inexistant').subscribe((r) => (result = r));

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([
      { id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`).flush([]);

    expect(result).toEqual([]);
  });
});
