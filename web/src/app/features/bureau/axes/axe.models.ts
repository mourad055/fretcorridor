/** EF-GEO-03 (MVP, priorite M) : les 3 etats sont independants, jamais un enum a valeur unique. */
export interface Axe {
  id: string;
  origine: string;
  destination: string;
  distanceKm: number;
  visibiliteActive: boolean;
  matchingActif: boolean;
  paiementActif: boolean;
}
