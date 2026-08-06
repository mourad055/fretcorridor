export type AxeEtatActivation = 'VISIBILITE' | 'MATCHING' | 'PAIEMENT';

export interface Axe {
  id: string;
  origine: string;
  destination: string;
  distanceKm: number;
  etatActivation: AxeEtatActivation;
}
