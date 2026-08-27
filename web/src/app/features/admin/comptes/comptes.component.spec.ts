import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { axe } from 'jest-axe';
import { ComptesComponent } from './comptes.component';
import { environment } from '../../../../environments/environment';

const COMPTE = {
  id: 'c1',
  telephone: '+237690000001',
  nom: 'Mbarga',
  prenom: 'Jean',
  raisonSociale: null,
  tenantId: 'tenant-bgft-douala',
  roles: ['BUREAU'],
  actif: true,
  niveauKyc: 'NIVEAU_1',
};

const CHAUFFEUR = {
  id: 'c2',
  telephone: '+237610000001',
  nom: 'bobo',
  prenom: 'toto',
  raisonSociale: null,
  tenantId: 'tenant-bgft-douala',
  roles: ['CHAUFFEUR'],
  actif: true,
  niveauKyc: 'NIVEAU_1',
};

describe('ComptesComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComptesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('charge les comptes au demarrage', () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();

    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([COMPTE]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Jean Mbarga');
  });

  it('ouvre la modale Voir', () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([COMPTE]);
    fixture.detectChanges();

    const voir = fixture.nativeElement.querySelector('button');
    const voirBtn = Array.from(fixture.nativeElement.querySelectorAll('button')).find((b: Element) =>
      b.textContent?.includes('Voir')
    ) as HTMLButtonElement;
    voirBtn.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Canal métier');
    expect(fixture.nativeElement.querySelector('.fc-modal')).toBeTruthy();
  });

  it('desactive via modale de confirmation', () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([COMPTE]);
    fixture.detectChanges();

    const desactiver = Array.from(fixture.nativeElement.querySelectorAll('button')).find((b: Element) =>
      b.textContent?.includes('Désactiver')
    ) as HTMLButtonElement;
    desactiver.click();
    fixture.detectChanges();

    const confirmer = Array.from(fixture.nativeElement.querySelectorAll('.fc-modal__footer button')).find((b: Element) =>
      b.textContent?.includes('Désactiver')
    ) as HTMLButtonElement;
    confirmer.click();

    const reqStatut = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes/c1/statut`);
    expect(reqStatut.request.body).toEqual({ actif: false });
    reqStatut.flush({ ...COMPTE, actif: false });
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([{ ...COMPTE, actif: false }]);
  });

  it('change les roles via modale', () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([COMPTE]);
    fixture.detectChanges();

    fixture.componentInstance.ouvrirEditionRoles(COMPTE as never);
    fixture.componentInstance.basculerRoleEdition('ADMINISTRATION', true);
    fixture.componentInstance.basculerRoleEdition('BUREAU', false);
    fixture.detectChanges();
    fixture.componentInstance.enregistrerRoles();

    const reqRoles = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes/c1/roles`);
    expect(reqRoles.request.body).toEqual({ roles: ['ADMINISTRATION'] });
    reqRoles.flush({ ...COMPTE, roles: ['ADMINISTRATION'] });
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([{ ...COMPTE, roles: ['ADMINISTRATION'] }]);
  });

  it('propose le lien KYC pour un chauffeur', () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([CHAUFFEUR]);
    fixture.detectChanges();

    fixture.componentInstance.voirCompte(CHAUFFEUR as never);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('App Chauffeur / Transporteur mobile');
    expect(fixture.nativeElement.textContent).toContain('Ouvrir la file KYC');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([COMPTE]);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
