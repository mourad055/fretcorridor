export type StatutMission = 'CONFIRMEE' | 'EN_COURS' | 'CLOTUREE';

export interface MissionAppariee {
  id: string;
  transporteurNom: string;
  origine: string;
  destination: string;
  enlevementLe: string;
  statut: StatutMission;
}
