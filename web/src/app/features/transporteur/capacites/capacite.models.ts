export type ModeCollecte = 'PORTE_A_PORTE' | 'POINT_DEPOT';
export type CapaciteEtat = 'PUBLIEE' | 'APPARIEE' | 'EXPIREE';

export interface Capacite {
  id: string;
  vehicule: string;
  origine: string;
  destination: string;
  departLe: string;
  poidsTaxableKg: number;
  modeCollecte: ModeCollecte;
  etat: CapaciteEtat;
}
