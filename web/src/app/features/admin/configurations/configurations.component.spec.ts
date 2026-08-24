import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { ConfigurationsComponent } from './configurations.component';
import { environment } from '../../../../environments/environment';

describe('ConfigurationsComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfigurationsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
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

    fixture.componentInstance.cle.set('seuil-agregation-bur');
    fixture.componentInstance.consulter();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([{ cle: 'seuil-agregation-bur', perimetre: 'GLOBAL', valeur: '3', auteur: 'actor-admin-1', version: 1, creeLe: '2026-08-05T00:00:00Z' }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('3');
  });

  it('definit une nouvelle version puis rafraichit l\'historique et le catalogue', () => {
    const fixture = creerEtIgnorerLeCatalogue();

    fixture.componentInstance.cle.set('seuil-agregation-bur');
    fixture.componentInstance.nouvelleValeur.set('5');
    fixture.componentInstance.definir();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur`).flush({});
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations`).flush([]);

    expect(fixture.componentInstance.nouvelleValeur()).toBe('');
  });

  it('ne fait aucun appel reseau si la confirmation est annulee', () => {
    (window.confirm as jest.Mock).mockReturnValue(false);
    const fixture = creerEtIgnorerLeCatalogue();

    fixture.componentInstance.cle.set('seuil-agregation-bur');
    fixture.componentInstance.nouvelleValeur.set('5');
    fixture.componentInstance.definir();

    httpMock.expectNone(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur`);
  });

  it('affiche un skeleton pendant la consultation puis le masque', () => {
    const fixture = creerEtIgnorerLeCatalogue();

    fixture.componentInstance.cle.set('seuil-agregation-bur');
    fixture.componentInstance.consulter();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.fc-skeleton')).toBeTruthy();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.fc-skeleton')).toBeFalsy();
  });

  it("affiche un état vide après une consultation sans version, pas avant", () => {
    const fixture = creerEtIgnorerLeCatalogue();

    expect(fixture.nativeElement.querySelector('.fc-empty')).toBeTruthy(); // catalogue vide

    fixture.componentInstance.cle.set('seuil-agregation-bur');
    fixture.componentInstance.consulter();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.fc-empty').length).toBe(2);
  });

  it('désactive les boutons de consultation/définition pendant le chargement', () => {
    const fixture = creerEtIgnorerLeCatalogue();

    fixture.componentInstance.cle.set('seuil-agregation-bur');
    fixture.componentInstance.consulter();
    fixture.detectChanges();

    const buttons = fixture.debugElement.queryAll(By.css('button'));
    buttons.forEach((button) => expect(button.nativeElement.disabled).toBe(true));

    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([]);
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
