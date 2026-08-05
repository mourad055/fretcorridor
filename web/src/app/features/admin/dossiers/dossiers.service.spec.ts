import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DossiersService } from './dossiers.service';
import { environment } from '../../../../environments/environment';

describe('DossiersService', () => {
  let service: DossiersService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DossiersService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the file de travail of a tenant', () => {
    let result: unknown;
    service.fileDeTravail('tenant-bgft-douala').subscribe((dossiers) => (result = dossiers));

    const req = httpMock.expectOne(
      (r) => r.url === `${environment.apiBaseUrl}/admin/dossiers` && r.params.get('tenantId') === 'tenant-bgft-douala'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);

    expect(result).toEqual([]);
  });

  it('takes a dossier in charge', () => {
    service.priseEnCharge('dossier-1').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/dossiers/dossier-1/prise-en-charge`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('sends a decision with its motif', () => {
    service.decider('dossier-1', 'RESOLU', 'motif').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/dossiers/dossier-1/decision`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ decision: 'RESOLU', motif: 'motif' });
    req.flush({});
  });

  it('triggers the automatic escalation', () => {
    service.declencherEscalade().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/dossiers/escalade`);
    expect(req.request.method).toBe('POST');
    req.flush([]);
  });
});
