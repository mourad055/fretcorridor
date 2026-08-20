export type TypeDossier = 'MODERATION' | 'INCIDENT' | 'LITIGE';
export type PrioriteDossier = 'BASSE' | 'NORMALE' | 'HAUTE';
export type StatutDossier = 'OUVERT' | 'EN_COURS' | 'ESCALADE' | 'CLOS';

export interface Dossier {
  id: string;
  tenantId: string;
  type: TypeDossier;
  priorite: PrioriteDossier;
  statut: StatutDossier;
  missionId: string | null;
  parties: string[];
  preuvesReferences: string[];
  ouvertLe: string;
  delaiTraitement: string;
  priseEnChargeParActeurId: string | null;
  decision: string | null;
  motifDecision: string | null;
  decidePar: string | null;
  decideLe: string | null;
}

export interface EtapeMission {
  rang: number;
  type: string;
  lieu: string;
  etat: string;
}

export interface MissionResume {
  id: string;
  transporteurNom: string;
  origine: string;
  destination: string;
  etapes: EtapeMission[];
}

export interface EcritureResume {
  id: string;
  missionId: string;
  typeCompte: string;
  nature: string;
  sens: string;
  montant: number;
  creeLe: string;
  statut: string;
}

export interface DossierConsolide {
  dossier: Dossier;
  mission: MissionResume | null;
  ecritures: EcritureResume[];
}
