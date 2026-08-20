import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NotificationsService } from './notifications.service';
import { environment } from '../../../../environments/environment';

describe('NotificationsService', () => {
  let service: NotificationsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(NotificationsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the notifications of the current tenant', () => {
    let result: unknown;
    service.list().subscribe((notifications) => (result = notifications));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/notifications`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 'not-1',
        canal: 'EMAIL',
        destinataire: 'bureau.douala@bgft.example',
        objet: 'Nouvelle mission appariée',
        resume: 'La mission Douala → Yaoundé a été appariée à un transporteur.',
        envoyeeLe: '2026-08-05T02:00:00Z',
      },
    ]);

    expect(result).toHaveLength(1);
  });
});
