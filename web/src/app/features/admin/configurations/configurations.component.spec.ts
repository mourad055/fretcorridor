import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
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

  afterEach(() => httpMock.verify());

  it('affiche l\'historique des versions après consultation', () => {
    const fixture = TestBed.createComponent(ConfigurationsComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulter();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([{ cle: 'seuil-agregation-bur', perimetre: 'GLOBAL', valeur: '3', auteur: 'actor-admin-1', version: 1, creeLe: '2026-08-05T00:00:00Z' }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('3');
  });

  it('definit une nouvelle version puis rafraichit l\'historique', () => {
    const fixture = TestBed.createComponent(ConfigurationsComponent);
    fixture.detectChanges();

    fixture.componentInstance.nouvelleValeur.set('5');
    fixture.componentInstance.definir();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur`).flush({});
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/configurations/seuil-agregation-bur/historique`)
      .flush([]);

    expect(fixture.componentInstance.nouvelleValeur()).toBe('');
  });
});
