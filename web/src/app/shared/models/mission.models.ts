export type EtapeType = 'ENLEVEMENT' | 'LIVRAISON';
export type EtapeEtat = 'A_VENIR' | 'EN_COURS' | 'TERMINEE';

export interface EtapeMission {
  rang: number;
  type: EtapeType;
  lieu: string;
  etat: EtapeEtat;
}

export interface Mission {
  id: string;
  transporteurNom: string;
  origine: string;
  destination: string;
  etapes: EtapeMission[];
}
