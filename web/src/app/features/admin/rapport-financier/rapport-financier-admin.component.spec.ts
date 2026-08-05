import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { RapportFinancierAdminComponent } from './rapport-financier-admin.component';
import { environment } from '../../../../environments/environment';

describe('RapportFinancierAdminComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RapportFinancierAdminComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('does not fetch anything until the admin clicks Consulter', () => {
    const fixture = TestBed.createComponent(RapportFinancierAdminComponent);
    fixture.detectChanges();

    httpMock.expectNone(`${environment.apiBaseUrl}/admin/rapport-financier/tenant-bgft-douala`);
  });

  it('fetches the report of the selected tenant on demand', () => {
    const fixture = TestBed.createComponent(RapportFinancierAdminComponent);
    fixture.detectChanges();

    fixture.componentInstance.consulter();

    httpMock.expectOne(`${environment.apiBaseUrl}/admin/rapport-financier/tenant-bgft-douala`).flush([
      { id: 'e1', missionId: 'mission-1', typeCompte: 'COMPTE_SEQUESTRE_PRESTATAIRE', nature: 'ENCAISSEMENT', sens: 'CREDIT', montant: 500, creeLe: '2026-01-01T00:00:00Z', statut: 'VALIDE' },
    ]);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('app-ecritures-table'))).toBeTruthy();
  });
});
