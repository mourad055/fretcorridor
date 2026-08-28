export type KycStatut = 'EN_ATTENTE' | 'VALIDE' | 'REJETE';
export type KycFiltre = 'pending' | 'NIVEAU_1' | 'NIVEAU_2';

export interface KycDossier {
  id: string;
  acteurNom: string;
  acteurTelephone: string;
  typeActeur: string;
  soumisLe: string;
  statut: KycStatut;
  niveauKyc?: string;
  roles?: string[];
}

export interface KycPiece {
  id: string;
  typeDocument: string;
  url: string;
  dateDepot: string | null;
}

export interface KycDetail {
  id: string;
  telephone: string;
  nom: string | null;
  prenom: string | null;
  raisonSociale: string | null;
  niveauKyc: string;
  roles: string[];
  pieces: KycPiece[];
}
