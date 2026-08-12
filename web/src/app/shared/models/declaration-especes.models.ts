/** EF-PAY-07 (S) : paiement en espèces déclaré — jamais une écriture de grand livre. */
export interface DeclarationEspeces {
  id: string;
  missionId: string;
  montant: number;
  declareeLe: string;
  protectionAssuree: boolean;
}
