import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { axe } from 'jest-axe';
import { ConfigurationsComponent } from './configurations.component';
import { environment } from '../../../../environments/environment';
import { ConfirmationService } from '../../../shared/services/confirmation.service';

describe('ConfigurationsComponent', () => {
  let httpMock: HttpTestingController;
  let confirmation: { confirmer: jest.Mock };

  beforeEach(async () => {
    confirmation = { confirmer: jest.fn().mockResolvedValue(true) };
    await TestBed.configureTestingModule({
      imports: [ConfigurationsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ConfirmationService, useValue: confirmation },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  /** Chaque test flush le catalogue chargé par ngOnInit avant de continuer. */
  function creerEtIgnorerLeCatalogue(catalogue: unknown[] = []) {
    const fixture = TestBed.createComponent(ConfigurationsComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations`).flush(catalogue);
    fixture.detectChanges();
    return fixture;
  }

  it('charge le catalogue des paramètres déjà configurés au démarrage', () => {
    const fixture = TestBed.createComponent(ConfigurationsComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations`)
      .flush([{ cle: 'grille-decision', perimetre: 'GLOBAL', valeur: '1', auteur: 'actor-admin-1', version: 1, creeLe: '2026-08-05T00:00:00Z' }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('grille-decision');
  });

  it('sélectionner une clé du catalogue en ouvre l\'historique', () => {
    const fixture = creerEtIgnorerLeCatalogue([
      { cle: 'grille-decision', perimetre: 'GLOBAL', valeur: '1', auteur: 'actor-admin-1', version: 1, creeLe: '2026-08-05T00:00:00Z' },
    ]);

    fixture.componentInstance.selectionner('grille-decision');
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/grille-decision/historique`)
      .flush([{ cle: 'grille-decision', perimetre: 'GLOBAL', valeur: '1', auteur: 'actor-admin-1', version: 1, creeLe: '2026-08-05T00:00:00Z' }]);
    fixture.detectChanges();

    expect(fixture.componentInstance.cle()).toBe('grille-decision');
  });

  it('affiche l\'historique des versions après consultation', () => {
    const fixture = creerEtIgnorerLeCatalogue();

    fixture.componentInstance.ouvrirHistorique('seuil-agregation-bur');
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([{ cle: 'seuil-agregation-bur', perimetre: 'GLOBAL', valeur: '3', auteur: 'actor-admin-1', version: 1, creeLe: '2026-08-05T00:00:00Z' }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('3');
    expect(fixture.nativeElement.querySelector('.fc-modal')).toBeTruthy();
  });

  it('definit une nouvelle version puis rafraichit l\'historique et le catalogue', async () => {
    const fixture = creerEtIgnorerLeCatalogue();

    fixture.componentInstance.cle.set('seuil-agregation-bur');
    fixture.componentInstance.nouvelleValeur.set('5');
    fixture.componentInstance.definir();
    await fixture.whenStable();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur`).flush({});
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations`).flush([]);

    expect(fixture.componentInstance.nouvelleValeur()).toBe('');
  });

  it('ne fait aucun appel reseau si la confirmation est annulee', async () => {
    confirmation.confirmer.mockResolvedValue(false);
    const fixture = creerEtIgnorerLeCatalogue();

    fixture.componentInstance.cle.set('seuil-agregation-bur');
    fixture.componentInstance.nouvelleValeur.set('5');
    fixture.componentInstance.definir();
    await fixture.whenStable();

    httpMock.expectNone(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur`);
  });

  it('affiche un skeleton pendant la consultation puis le masque', () => {
    const fixture = creerEtIgnorerLeCatalogue();

    fixture.componentInstance.ouvrirHistorique('seuil-agregation-bur');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.fc-modal .fc-skeleton')).toBeTruthy();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.fc-modal .fc-skeleton')).toBeFalsy();
  });

  it("affiche un état vide après une consultation sans version, pas avant", () => {
    const fixture = creerEtIgnorerLeCatalogue();

    expect(fixture.nativeElement.querySelector('.fc-empty')).toBeTruthy(); // catalogue vide

    fixture.componentInstance.ouvrirHistorique('seuil-agregation-bur');
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.fc-modal .fc-empty').length).toBe(1);
  });

  it('désactive le bouton Enregistrer pendant le chargement de la nouvelle version', async () => {
    const fixture = creerEtIgnorerLeCatalogue();

    fixture.componentInstance.ouvrirDefinition('seuil-agregation-bur', '5');
    fixture.detectChanges();

    fixture.componentInstance.definir();
    await fixture.whenStable();
    fixture.detectChanges();

    const enregistrer = Array.from(fixture.nativeElement.querySelectorAll('.fc-modal button')).find((b: Element) =>
      b.textContent?.includes('Enregistrer')
    ) as HTMLButtonElement;
    expect(enregistrer.disabled).toBe(true);

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur`).flush({});
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations`).flush([]);
  });

  it('ne consulte rien si aucune clé n\'est saisie', () => {
    const fixture = creerEtIgnorerLeCatalogue();

    fixture.componentInstance.consulter();

    httpMock.expectNone(`${environment.apiBaseUrl}/admin/configurations//historique`);
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = creerEtIgnorerLeCatalogue([
      { cle: 'seuil-agregation-bur', perimetre: 'GLOBAL', valeur: '3', auteur: 'actor-admin-1', version: 1, creeLe: '2026-08-05T00:00:00Z' },
    ]);

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
