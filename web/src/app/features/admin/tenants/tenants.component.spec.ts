import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TenantsComponent } from './tenants.component';
import { environment } from '../../../../environments/environment';

describe('TenantsComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TenantsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche les tenants au chargement', () => {
    const fixture = TestBed.createComponent(TenantsComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/admin/tenants`)
      .flush([{ id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun' }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Bureau Douala');
  });

  it('cree un tenant puis rafraichit la liste', () => {
    const fixture = TestBed.createComponent(TenantsComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([]);

    fixture.componentInstance.nouvelId.set('tenant-new');
    fixture.componentInstance.nouvelNom.set('Bureau Neuf');
    fixture.componentInstance.nouveauPays.set('Tchad');
    fixture.componentInstance.creer();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush({});
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([]);

    expect(fixture.componentInstance.nouvelId()).toBe('');
  });
});
