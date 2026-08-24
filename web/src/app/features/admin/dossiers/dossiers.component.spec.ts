import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { DossiersComponent } from './dossiers.component';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

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
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateServiceForTests()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  beforeEach(() => {
    jest.spyOn(window, 'confirm').mockReturnValue(true);
  });

  afterEach(() => {
    httpMock.verify();
    jest.restoreAllMocks();
  });

  it('affiche la file de travail après consultation', () => {
    const fixture = TestBed.createComponent(DossiersComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulterFileDeTravail();
    httpMock
      .expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`)
      .flush([DOSSIER]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Litige');
    expect(fixture.nativeElement.textContent).toContain('1 dossier(s) en attente, dont 1 en retard');
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

  it('ne tranche rien si la confirmation est annulee', () => {
    (window.confirm as jest.Mock).mockReturnValue(false);
    const fixture = TestBed.createComponent(DossiersComponent);
    fixture.detectChanges();

    fixture.componentInstance.ouvrirDossier('dossier-1');
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/dossiers/dossier-1`)
      .flush({ dossier: DOSSIER, mission: null, ecritures: [] });
    fixture.detectChanges();

    fixture.componentInstance.decisionTexte.set('RESOLU');
    fixture.componentInstance.motifTexte.set('Preuve conforme');
    fixture.componentInstance.trancher();

    httpMock.expectNone(`${environment.apiBaseUrl}/admin/dossiers/dossier-1/decision`);
  });

  it('désactive les actions de la ligne pendant la prise en charge', () => {
    const fixture = TestBed.createComponent(DossiersComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulterFileDeTravail();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`).flush([DOSSIER]);
    fixture.detectChanges();

    const [prendreEnCharge, voirDossier] = fixture.debugElement.queryAll(By.css('tbody button'));
    prendreEnCharge.nativeElement.click();
    fixture.detectChanges();

    expect(prendreEnCharge.nativeElement.disabled).toBe(true);
    expect(voirDossier.nativeElement.disabled).toBe(true);

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/dossiers/dossier-1/prise-en-charge`).flush({});
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`).flush([]);
  });

  it('désactive le bouton Trancher pendant la décision', () => {
    const fixture = TestBed.createComponent(DossiersComponent);
    fixture.detectChanges();

    fixture.componentInstance.ouvrirDossier('dossier-1');
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/dossiers/dossier-1`)
      .flush({ dossier: DOSSIER, mission: null, ecritures: [] });
    fixture.detectChanges();

    fixture.componentInstance.decisionTexte.set('RESOLU');
    fixture.componentInstance.motifTexte.set('Preuve conforme');
    fixture.componentInstance.trancher();
    fixture.detectChanges();

    expect(fixture.componentInstance.trancheEnCours()).toBe(true);

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/dossiers/dossier-1/decision`).flush({ ...DOSSIER, statut: 'CLOS' });
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`).flush([]);

    expect(fixture.componentInstance.trancheEnCours()).toBe(false);
  });

  it('désactive Trancher tant que la décision ou le motif est vide, et ne fait aucun appel réseau si on force malgré tout', () => {
    const fixture = TestBed.createComponent(DossiersComponent);
    fixture.detectChanges();

    fixture.componentInstance.ouvrirDossier('dossier-1');
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/dossiers/dossier-1`)
      .flush({ dossier: DOSSIER, mission: null, ecritures: [] });
    fixture.detectChanges();

    const trancherBtn = fixture.debugElement.query(By.css('.field__fieldset .fc-btn--primary'));
    expect(trancherBtn.nativeElement.disabled).toBe(true);

    fixture.componentInstance.decisionTexte.set('   ');
    fixture.componentInstance.motifTexte.set('Preuve conforme');
    fixture.detectChanges();
    expect(trancherBtn.nativeElement.disabled).toBe(true);

    fixture.componentInstance.trancher();
    httpMock.expectNone(`${environment.apiBaseUrl}/admin/dossiers/dossier-1/decision`);

    fixture.componentInstance.decisionTexte.set('RESOLU');
    fixture.detectChanges();
    expect(trancherBtn.nativeElement.disabled).toBe(false);
  });

  it("désactive le bouton d'escalade pendant la détection", () => {
    const fixture = TestBed.createComponent(DossiersComponent);
    fixture.detectChanges();

    fixture.componentInstance.declencherEscalade();
    fixture.detectChanges();

    expect(fixture.componentInstance.escaladeEnCours()).toBe(true);

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/dossiers/escalade`).flush({});
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`).flush([]);

    expect(fixture.componentInstance.escaladeEnCours()).toBe(false);
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(DossiersComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulterFileDeTravail();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`).flush([DOSSIER]);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
