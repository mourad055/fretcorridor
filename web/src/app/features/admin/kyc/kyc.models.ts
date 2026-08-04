export type KycStatut = 'EN_ATTENTE' | 'VALIDE' | 'REJETE';

export interface KycDossier {
  id: string;
  acteurNom: string;
  acteurTelephone: string;
  typeActeur: string;
  soumisLe: string;
  statut: KycStatut;
}
