import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { axe } from 'jest-axe';
import { TenantsComponent } from './tenants.component';
import { environment } from '../../../../environments/environment';
import { ConfirmationService } from '../../../shared/services/confirmation.service';

describe('TenantsComponent', () => {
  let httpMock: HttpTestingController;
  let confirmation: { confirmer: jest.Mock };

  beforeEach(async () => {
    confirmation = { confirmer: jest.fn().mockResolvedValue(true) };
    await TestBed.configureTestingModule({
      imports: [TenantsComponent],
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

  it('affiche les tenants au chargement, avec leur statut', () => {
    const fixture = TestBed.createComponent(TenantsComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/tenants`)
      .flush([{ id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Bureau Douala');
    expect(fixture.nativeElement.textContent).toContain('Actif');
    expect(fixture.nativeElement.textContent).toContain('1 tenant(s), dont 1 actif(s)');
  });

  it('cree un tenant puis rafraichit la liste', async () => {
    const fixture = TestBed.createComponent(TenantsComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([]);

    fixture.componentInstance.nouvelId.set('tenant-new');
    fixture.componentInstance.nouvelNom.set('Bureau Neuf');
    fixture.componentInstance.nouveauPays.set('Tchad');
    fixture.componentInstance.creer();
    await fixture.whenStable();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush({});
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([]);

    expect(fixture.componentInstance.nouvelId()).toBe('');
  });

  it('ne cree rien si la confirmation est annulee', async () => {
    confirmation.confirmer.mockResolvedValue(false);
    const fixture = TestBed.createComponent(TenantsComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([]);

    fixture.componentInstance.nouvelId.set('tenant-new');
    fixture.componentInstance.nouvelNom.set('Bureau Neuf');
    fixture.componentInstance.nouveauPays.set('Tchad');
    fixture.componentInstance.creer();
    await fixture.whenStable();

    httpMock.expectNone((r) => r.url === `${environment.apiBaseUrl}/admin/tenants` && r.method === 'POST');
  });

  it('filtre les tenants affiches par la recherche', () => {
    const fixture = TestBed.createComponent(TenantsComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([
      { id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true },
      { id: 'tenant-bnft-ndjamena', nom: 'Bureau N\'Djamena', pays: 'Tchad', actif: true },
    ]);
    fixture.detectChanges();

    fixture.componentInstance.recherche.set('Tchad');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('N\'Djamena');
    expect(fixture.nativeElement.textContent).not.toContain('Bureau Douala');
  });

  it('modifie un tenant existant (nom, pays, statut) puis rafraichit la liste', () => {
    const fixture = TestBed.createComponent(TenantsComponent);
    fixture.detectChanges();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/tenants`)
      .flush([{ id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true }]);
    fixture.detectChanges();

    fixture.componentInstance.commencerEdition({ id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true });
    fixture.componentInstance.editNom.set('Bureau Douala renommé');
    fixture.componentInstance.editActif.set(false);
    fixture.componentInstance.enregistrerEdition();

    const reqModif = httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants/tenant-bgft-douala`);
    expect(reqModif.request.method).toBe('PUT');
    expect(reqModif.request.body).toEqual({ nom: 'Bureau Douala renommé', pays: 'Cameroun', actif: false });
    reqModif.flush({});

    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/tenants`)
      .flush([{ id: 'tenant-bgft-douala', nom: 'Bureau Douala renommé', pays: 'Cameroun', actif: false }]);

    expect(fixture.componentInstance.tenantEnEdition()).toBeNull();
  });

  it('pagine au-dela de 20 tenants et revient a la page 1 apres une recherche', () => {
    const fixture = TestBed.createComponent(TenantsComponent);
    fixture.detectChanges();
    const beaucoup = Array.from({ length: 22 }, (_, i) => ({
      id: `tenant-${i}`,
      nom: `Bureau ${i}`,
      pays: 'Cameroun',
      actif: true,
    }));
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush(beaucoup);
    fixture.detectChanges();

    expect(fixture.componentInstance.tenantsAffiches()).toHaveLength(20);

    fixture.componentInstance.page.set(2);
    fixture.componentInstance.onRechercheChange('Bureau');
    expect(fixture.componentInstance.page()).toBe(1);
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(TenantsComponent);
    fixture.detectChanges();
    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/tenants`)
      .flush([{ id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true }]);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
