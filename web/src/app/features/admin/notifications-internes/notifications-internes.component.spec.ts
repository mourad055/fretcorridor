import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { axe } from 'jest-axe';
import { NotificationsInternesComponent } from './notifications-internes.component';
import { environment } from '../../../../environments/environment';

describe('NotificationsInternesComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationsInternesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function flushSansDossier(): void {
    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([
      { id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true },
    ]);
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`).flush([]);
  }

  it('affiche un message si aucun dossier en retard', () => {
    const fixture = TestBed.createComponent(NotificationsInternesComponent);
    fixture.detectChanges();
    flushSansDossier();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun dossier en retard');
  });

  it('affiche les alertes de dossiers en retard', () => {
    const fixture = TestBed.createComponent(NotificationsInternesComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/tenants`).flush([
      { id: 'tenant-bgft-douala', nom: 'Bureau Douala', pays: 'Cameroun', actif: true },
    ]);
    httpMock.expectOne((r) => r.url === `${environment.apiBaseUrl}/admin/dossiers`).flush([
      { id: 'd1', tenantId: 'tenant-bgft-douala', type: 'LITIGE', priorite: 'HAUTE', statut: 'OUVERT', missionId: 'm1', parties: [], preuvesReferences: [], ouvertLe: '2020-01-01T00:00:00Z', delaiTraitement: '2020-01-02T00:00:00Z', priseEnChargeParActeurId: null, decision: null, motifDecision: null, decidePar: null, decideLe: null },
    ]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Dossier en retard — Bureau Douala');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(NotificationsInternesComponent);
    fixture.detectChanges();
    flushSansDossier();
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
