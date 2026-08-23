import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ObservatoireService } from './observatoire.service';
import { environment } from '../../../../environments/environment';

describe('ObservatoireService', () => {
  let service: ObservatoireService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ObservatoireService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it("recupere les indicateurs de marche d'un axe", () => {
    let result: unknown;
    service.observatoirePourAxe('axe-1').subscribe((vue) => (result = vue));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/observatoire/axe-1`);
    expect(req.request.method).toBe('GET');
    req.flush({
      axeId: 'axe-1',
      seuil: 5,
      seuilAtteint: true,
      nombreMissions: 12,
      prixMediane: 45000,
      prixDispersion: 3000,
      devise: 'XAF',
      tauxDesequilibreDirectionnel: 0.2,
      couverturePourcentage: 60,
      estimationDefinieLe: '2026-08-01T00:00:00Z',
    });

    expect(result).toMatchObject({ axeId: 'axe-1', seuilAtteint: true, nombreMissions: 12 });
  });

  it('declare une estimation de marche', () => {
    service.definirEstimationMarche('axe-1', 100, 'Enquête bureau').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/observatoire/axe-1/estimation-marche`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ volumeMensuelEstime: 100, source: 'Enquête bureau' });
    req.flush(null);
  });

  it('liste les alertes de seuil', () => {
    let result: unknown;
    service.listerAlertes().subscribe((alertes) => (result = alertes));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/alertes`);
    expect(req.request.method).toBe('GET');
    req.flush([]);

    expect(result).toEqual([]);
  });

  it("recupere l'etat des alertes", () => {
    let result: unknown;
    service.etatAlertes().subscribe((etats) => (result = etats));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/alertes/etat`);
    expect(req.request.method).toBe('GET');
    req.flush([]);

    expect(result).toEqual([]);
  });

  it('configure une nouvelle alerte', () => {
    service.configurerAlerte('axe-1', 'PRIX_MEDIANE', 'SUPERIEUR', 50000).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/alertes`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      axeId: 'axe-1',
      indicateur: 'PRIX_MEDIANE',
      comparateur: 'SUPERIEUR',
      seuil: 50000,
    });
    req.flush({});
  });

  it('supprime une alerte', () => {
    service.supprimerAlerte('alerte-1').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/alertes/alerte-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
