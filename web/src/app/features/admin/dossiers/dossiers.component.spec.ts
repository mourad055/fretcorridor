import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DossiersComponent } from './dossiers.component';
import { environment } from '../../../../environments/environment';

const DOSSIER = {
  id: 'dossier-1',
  tenantId: 'tenant-bgft-douala',
  type: 'LITIGE',
  priorite: 'HAUTE',
  statut: 'OUVERT',
  missionId: 'mission-a',
  parties: ['acteur-transporteur-1'],
  preuvesReferences: [],
  ouvertLe: '2026-08-05T00:00:00Z',
  delaiTraitement: '2026-08-06T00:00:00Z',
  priseEnChargeParActeurId: null,
  decision: null,
  motifDecision: null,
  decidePar: null,
  decideLe: null,
};

describe('DossiersComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DossiersComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche la file de travail après consultation', () => {
    const fixture = TestBed.createComponent(DossiersComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulterFileDeTravail();
    httpMock
      .expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`)
      .flush([DOSSIER]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('LITIGE');
  });

  it('ouvre le dossier consolidé et permet de trancher', () => {
    const fixture = TestBed.createComponent(DossiersComponent);
    fixture.detectChanges();

    fixture.componentInstance.ouvrirDossier('dossier-1');
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/dossiers/dossier-1`)
      .flush({
        dossier: DOSSIER,
        mission: { id: 'mission-a', transporteurNom: 'Transport Étoile', origine: 'Douala', destination: 'Yaoundé', etapes: [] },
        ecritures: [],
      });
    fixture.detectChanges();

    expect(fixture.componentInstance.dossierConsolide()).not.toBeNull();

    fixture.componentInstance.decisionTexte.set('RESOLU');
    fixture.componentInstance.motifTexte.set('Preuve conforme');
    fixture.componentInstance.trancher();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/dossiers/dossier-1/decision`);
    expect(req.request.body).toEqual({ decision: 'RESOLU', motif: 'Preuve conforme' });
    req.flush({ ...DOSSIER, statut: 'CLOS' });

    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`).flush([]);
  });
});
