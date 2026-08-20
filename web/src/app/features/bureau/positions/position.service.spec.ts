import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { PositionService } from './position.service';
import { environment } from '../../../../environments/environment';

describe('PositionService', () => {
  let service: PositionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PositionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the positions of the current tenant', () => {
    let result: unknown;
    service.list().subscribe((positions) => (result = positions));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/bureau/positions`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 'pos-1',
        vehiculeLabel: 'Camion 10T',
        latitude: 4.05,
        longitude: 9.76,
        capturedLe: '2026-01-01T00:00:00Z',
        ageSecondes: 90,
      },
    ]);

    expect(result).toHaveLength(1);
  });
});
