export type StatutMission = 'CONFIRMEE' | 'EN_COURS' | 'CLOTUREE';

export interface MissionAppariee {
  id: string;
  axeId: string;
  transporteurNom: string;
  origine: string;
  destination: string;
  enlevementLe: string;
  statut: StatutMission;
}

/** EF-BUR-02 : filtrage des flux supervisés — les deux critères sont optionnels et cumulables. */
export interface MissionsFiltre {
  statut?: StatutMission;
  axeId?: string;
}
