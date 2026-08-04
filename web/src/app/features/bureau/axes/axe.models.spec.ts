import { Axe, layoutHubs, layoutSegments } from './axe.models';

const AXES: Axe[] = [
  { id: 'axe-1', origine: 'Douala', destination: 'Yaoundé', distanceKm: 300, etatActivation: 'PAIEMENT' },
  { id: 'axe-2', origine: 'Douala', destination: 'Bafoussam', distanceKm: 350, etatActivation: 'MATCHING' },
];

describe('layoutHubs', () => {
  it('deduplicates hubs shared by several axes', () => {
    const hubs = layoutHubs(AXES);

    expect(hubs.map((h) => h.nom).sort()).toEqual(['Bafoussam', 'Douala', 'Yaoundé']);
  });

  it('returns an empty layout for no axes', () => {
    expect(layoutHubs([])).toEqual([]);
  });
});

describe('layoutSegments', () => {
  it('links each axe to its computed hub positions', () => {
    const hubs = layoutHubs(AXES);
    const segments = layoutSegments(AXES, hubs);

    expect(segments).toHaveLength(2);
    const first = segments.find((s) => s.axe.id === 'axe-1')!;
    const doualaHub = hubs.find((h) => h.nom === 'Douala')!;
    const yaoundeHub = hubs.find((h) => h.nom === 'Yaoundé')!;
    expect(first.x1).toBe(doualaHub.x);
    expect(first.x2).toBe(yaoundeHub.x);
  });
});
