import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { axe } from 'jest-axe';
import { RechercheComponent } from './recherche.component';
import { environment } from '../../../../environments/environment';

describe('RechercheComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RechercheComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('ne recherche rien tant que le terme est vide', () => {
    const fixture = TestBed.createComponent(RechercheComponent);
    fixture.detectChanges();

    fixture.componentInstance.rechercher();

    httpMock.expectNone(`${environment.apiBaseUrl}/admin/tenants`);
  });

  it('affiche les resultats apres recherche', () => {
    const fixture = TestBed.createComponent(RechercheComponent);
    fixture.detectChanges();

    fixture.componentInstance.terme.set('douala');
    fixture.componentInstance.rechercher();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([
      { id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true },
    ]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Bureau Douala');
  });

  it('affiche un message si aucun resultat', () => {
    const fixture = TestBed.createComponent(RechercheComponent);
    fixture.detectChanges();

    fixture.componentInstance.terme.set('inexistant');
    fixture.componentInstance.rechercher();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([]);
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/journal-audit`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun résultat');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(RechercheComponent);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
