import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { axe } from 'jest-axe';
import { NotificationsComponent } from './notifications.component';
import { environment } from '../../../../environments/environment';

const NOTIFICATIONS = [
  {
    id: 'not-1',
    canal: 'EMAIL',
    destinataire: 'bureau.douala@bgft.example',
    objet: 'Nouvelle mission appariée',
    resume: 'La mission Douala → Yaoundé a été appariée à un transporteur.',
    envoyeeLe: '2026-08-05T02:00:00Z',
  },
];

describe('NotificationsComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche les notifications du tenant au chargement', () => {
    const fixture = TestBed.createComponent(NotificationsComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/notifications`).flush(NOTIFICATIONS);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nouvelle mission appariée');
    expect(fixture.nativeElement.textContent).toContain('EMAIL');
  });

  it("affiche un message si aucune notification n'existe", () => {
    const fixture = TestBed.createComponent(NotificationsComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/notifications`).flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune notification');
  });

  it("affiche un message d'erreur si le chargement echoue", () => {
    const fixture = TestBed.createComponent(NotificationsComponent);
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiBaseUrl}/bureau/notifications`)
      .flush({ title: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alert.textContent).toContain('Impossible de charger');
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(NotificationsComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiBaseUrl}/bureau/notifications`).flush(NOTIFICATIONS);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
