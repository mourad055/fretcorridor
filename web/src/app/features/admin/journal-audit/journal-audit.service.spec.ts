import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { JournalAuditService } from './journal-audit.service';
import { environment } from '../../../../environments/environment';

describe('JournalAuditService', () => {
  let service: JournalAuditService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(JournalAuditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the journal entries', () => {
    let result: unknown;
    service.lister().subscribe((entrees) => (result = entrees));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`);
    expect(req.request.method).toBe('GET');
    req.flush([]);

    expect(result).toEqual([]);
  });

  it('exports the journal as csv text', () => {
    let result: unknown;
    service.exporterCsv().subscribe((csv) => (result = csv));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit/export`);
    expect(req.request.method).toBe('GET');
    req.flush('id,tenantId,acteurId,action,ressource,horodatage\n');

    expect(result).toContain('id,tenantId');
  });
});
