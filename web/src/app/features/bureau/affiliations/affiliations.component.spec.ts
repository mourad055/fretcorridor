import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { axe } from 'jest-axe';
import { AffiliationsComponent } from './affiliations.component';
import { ConfirmationService } from '../../../shared/services/confirmation.service';
import { environment } from '../../../../environments/environment';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

describe('AffiliationsComponent', () => {
  let httpMock: HttpTestingController;
  let confirmation: { confirmer: jest.Mock };

  beforeEach(async () => {
    confirmation = { confirmer: jest.fn().mockReturnValue(true) };
    await TestBed.configureTestingModule({
      imports: [AffiliationsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateServiceForTests(),
        { provide: ConfirmationService, useValue: confirmation },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('invite un transporteur puis affiche une confirmation et vide le champ', () => {
    const fixture = TestBed.createComponent(AffiliationsComponent);
    fixture.detectChanges();

    fixture.componentInstance.telephone.set('+237690000001');
    fixture.componentInstance.inviter();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/affiliations`);
    req.flush(null, { status: 201, statusText: 'Created' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('+237690000001 a été rattaché');
    expect(fixture.componentInstance.telephone()).toBe('');
  });

  it("n'appelle pas le serveur si le telephone est vide", () => {
    const fixture = TestBed.createComponent(AffiliationsComponent);
    fixture.detectChanges();

    fixture.componentInstance.inviter();

    httpMock.expectNone(`${environment.apiBaseUrl}/bureau/affiliations`);
    expect(confirmation.confirmer).not.toHaveBeenCalled();
  });

  it("n'appelle pas le serveur si la confirmation est refusee", () => {
    confirmation.confirmer.mockReturnValue(false);
    const fixture = TestBed.createComponent(AffiliationsComponent);
    fixture.detectChanges();

    fixture.componentInstance.telephone.set('+237690000001');
    fixture.componentInstance.inviter();

    httpMock.expectNone(`${environment.apiBaseUrl}/bureau/affiliations`);
  });

  it("affiche un message specifique quand l'acteur est introuvable (400)", () => {
    const fixture = TestBed.createComponent(AffiliationsComponent);
    fixture.detectChanges();

    fixture.componentInstance.telephone.set('+237699999999');
    fixture.componentInstance.inviter();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/bureau/affiliations`)
      .flush({ detail: 'ACTEUR_INTROUVABLE' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun compte ne correspond à ce numéro');
  });

  it("affiche un message specifique quand le role n'est pas affiliable (400)", () => {
    const fixture = TestBed.createComponent(AffiliationsComponent);
    fixture.detectChanges();

    fixture.componentInstance.telephone.set('+237600000003');
    fixture.componentInstance.inviter();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/bureau/affiliations`)
      .flush({ detail: 'ROLE_NON_AFFILIABLE' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('ne correspond pas à un chauffeur ou un transporteur');
  });

  it('affiche un message de service indisponible sur un 503', () => {
    const fixture = TestBed.createComponent(AffiliationsComponent);
    fixture.detectChanges();

    fixture.componentInstance.telephone.set('+237690000001');
    fixture.componentInstance.inviter();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/bureau/affiliations`)
      .flush({ detail: "Service d'identité indisponible" }, { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('indisponible');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(AffiliationsComponent);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
