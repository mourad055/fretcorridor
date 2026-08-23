import { TestBed } from '@angular/core/testing';
import { PositionsMapComponent } from './positions-map.component';
import { Position } from './position.models';

/**
 * Leaflet est doublé en test (src/testing/leaflet.mock.ts), même principe
 * que CorridorMapComponent.spec.ts : on vérifie l'initialisation/mise à jour
 * sans throw, pas le rendu visuel (couvert manuellement / en E2E).
 */
describe('PositionsMapComponent', () => {
  const POSITIONS: Position[] = [
    { id: 'p1', vehiculeLabel: 'Camion 10T — LT 1234 AB', latitude: 4.05, longitude: 9.76, capturedLe: '2026-08-23T00:00:00Z', ageSecondes: 30 },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PositionsMapComponent],
    }).compileComponents();
  });

  it('renders the map host without throwing', () => {
    const fixture = TestBed.createComponent(PositionsMapComponent);
    fixture.componentInstance.positions = POSITIONS;
    expect(() => fixture.detectChanges()).not.toThrow();

    expect(fixture.nativeElement.querySelector('.positions-map__host')).toBeTruthy();
  });

  it('handles an empty position list without throwing', () => {
    const fixture = TestBed.createComponent(PositionsMapComponent);
    fixture.componentInstance.positions = [];
    expect(() => fixture.detectChanges()).not.toThrow();
  });

  it('redraws without throwing when positions change after init', () => {
    const fixture = TestBed.createComponent(PositionsMapComponent);
    fixture.componentInstance.positions = [];
    fixture.detectChanges();

    fixture.componentInstance.positions = POSITIONS;
    expect(() => fixture.detectChanges()).not.toThrow();
  });
});
