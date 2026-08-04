export type AxeEtatActivation = 'VISIBILITE' | 'MATCHING' | 'PAIEMENT';

export interface Axe {
  id: string;
  origine: string;
  destination: string;
  distanceKm: number;
  etatActivation: AxeEtatActivation;
}

export interface HubPosition {
  nom: string;
  x: number;
  y: number;
}

export interface AxeSegment {
  axe: Axe;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  midX: number;
  midY: number;
}

/** Disposition schématique déterministe : les hubs uniques sont répartis sur une ligne. Ce n'est pas une carte géographique — voir docs/adr/0007. */
export function layoutHubs(axes: Axe[]): HubPosition[] {
  const noms = Array.from(new Set(axes.flatMap((a) => [a.origine, a.destination]))).sort();
  const width = 600;
  const margin = 60;
  const step = noms.length > 1 ? (width - 2 * margin) / (noms.length - 1) : 0;
  return noms.map((nom, index) => ({ nom, x: margin + index * step, y: 100 }));
}

export function layoutSegments(axes: Axe[], hubs: HubPosition[]): AxeSegment[] {
  const byName = new Map(hubs.map((h) => [h.nom, h]));
  return axes.map((axe) => {
    const from = byName.get(axe.origine)!;
    const to = byName.get(axe.destination)!;
    return {
      axe,
      x1: from.x,
      y1: from.y,
      x2: to.x,
      y2: to.y,
      midX: (from.x + to.x) / 2,
      midY: (from.y + to.y) / 2 - 12,
    };
  });
}
