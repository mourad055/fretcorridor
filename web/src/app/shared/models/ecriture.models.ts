export interface Ecriture {
  id: string;
  missionId: string;
  typeCompte: string;
  nature: string;
  sens: string;
  montant: number;
  creeLe: string;
  statut: string;
  modePaiement: string | null;
  litigeActif: boolean;
}
