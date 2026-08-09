import { TestBed } from '@angular/core/testing';
import { CorridorMapComponent } from './corridor-map.component';
import { Axe } from './axe.models';

/**
 * Leaflet est doublé en test (src/testing/leaflet.mock.ts) : jsdom ne
 * supporte pas assez le rendu SVG/Canvas pour la vraie bibliothèque. Ces
 * tests vérifient que le composant s'initialise et se met à jour sans
 * erreur avec des axes réels et inconnus — pas le rendu visuel lui-même,
 * couvert manuellement / en E2E.
 */
describe('CorridorMapComponent', () => {
  const AXES: Axe[] = [
    { id: 'axe-1', origine: 'Douala', destination: 'Yaoundé', distanceKm: 300, visibiliteActive: true, matchingActif: true, paiementActif: true },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CorridorMapComponent],
    }).compileComponents();
  });

  it('renders the map host without throwing for known cities', () => {
    const fixture = TestBed.createComponent(CorridorMapComponent);
    fixture.componentInstance.axes = AXES;
    expect(() => fixture.detectChanges()).not.toThrow();

    expect(fixture.nativeElement.querySelector('.corridor-map__host')).toBeTruthy();
  });

  it('ignores axes referencing an unknown city without throwing', () => {
    const fixture = TestBed.createComponent(CorridorMapComponent);
    fixture.componentInstance.axes = [
      { id: 'axe-x', origine: 'Ville Inconnue', destination: 'Douala', distanceKm: 10, visibiliteActive: true, matchingActif: false, paiementActif: false },
    ];

    expect(() => fixture.detectChanges()).not.toThrow();
  });

  it('redraws without throwing when axes change after init', () => {
    const fixture = TestBed.createComponent(CorridorMapComponent);
    fixture.componentInstance.axes = [];
    fixture.detectChanges();

    fixture.componentInstance.axes = AXES;
    expect(() => fixture.detectChanges()).not.toThrow();
  });
});
