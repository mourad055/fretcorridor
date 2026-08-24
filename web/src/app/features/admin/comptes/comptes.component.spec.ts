import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
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

describe('ComptesComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComptesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('ne charge rien tant que Consulter n\'a pas ete clique', () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();

    httpMock.expectNone((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`);
  });

  it('affiche les comptes du tenant apres consultation', () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulter();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([COMPTE]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Jean Mbarga');
    expect(fixture.nativeElement.textContent).toContain('Actif');
  });

  it('bascule le statut d\'un compte puis rafraichit la liste', () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();
    fixture.componentInstance.consulter();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([COMPTE]);
    fixture.detectChanges();

    fixture.componentInstance.basculerStatut(COMPTE as never);

    const reqStatut = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes/c1/statut`);
    expect(reqStatut.request.body).toEqual({ actif: false });
    reqStatut.flush({ ...COMPTE, actif: false });

    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([{ ...COMPTE, actif: false }]);
  });

  it('change les roles d\'un compte via le formulaire d\'edition', () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();
    fixture.componentInstance.consulter();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([COMPTE]);
    fixture.detectChanges();

    fixture.componentInstance.commencerEditionRoles(COMPTE as never);
    fixture.componentInstance.basculerRoleEdition('ADMINISTRATION', true);
    fixture.componentInstance.basculerRoleEdition('BUREAU', false);
    fixture.componentInstance.enregistrerRoles();

    const reqRoles = httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes/c1/roles`);
    expect(reqRoles.request.body).toEqual({ roles: ['ADMINISTRATION'] });
    reqRoles.flush({ ...COMPTE, roles: ['ADMINISTRATION'] });

    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([{ ...COMPTE, roles: ['ADMINISTRATION'] }]);

    expect(fixture.componentInstance.compteEnEdition()).toBeNull();
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(ComptesComponent);
    fixture.detectChanges();
    fixture.componentInstance.consulter();
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/comptes`).flush([COMPTE]);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
