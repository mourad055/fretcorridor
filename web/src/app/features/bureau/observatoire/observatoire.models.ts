/** EF-BUR-07 (S) : indicateur de l'observatoire (EF-BUR-03) sur lequel une alerte peut être configurée. */
export type Indicateur = 'NOMBRE_MISSIONS' | 'PRIX_MEDIANE' | 'TAUX_DESEQUILIBRE_DIRECTIONNEL';

export type Comparateur = 'SUPERIEUR' | 'INFERIEUR';

/**
 * EF-BUR-03, UC-BUR-02 : indicateurs de marché d'un axe. Les champs
 * indicateurs sont `null` tant que le seuil d'agrégation n'est pas atteint
 * (EF-BUR-04, RG-085 — jamais de donnée ré-identifiante en dessous) ;
 * `couverturePourcentage`/`estimationDefinieLe` restent `null` tant qu'aucune
 * estimation de marché n'a été déclarée (EF-BUR-05, RG-087).
 */
export interface ObservatoireAxe {
  axeId: string;
  seuil: number;
  seuilAtteint: boolean;
  nombreMissions: number | null;
  prixMediane: number | null;
  prixDispersion: number | null;
  devise: string | null;
  tauxDesequilibreDirectionnel: number | null;
  couverturePourcentage: number | null;
  estimationDefinieLe: string | null;
}

/** EF-BUR-07 (S), UC-BUR-02 A1 : alerte de marché configurée par un agent sur un indicateur de l'observatoire d'un axe. */
export interface AlerteSeuil {
  id: string;
  axeId: string;
  indicateur: Indicateur;
  comparateur: Comparateur;
  seuil: number;
  creeParActeurId: string;
  creeLe: string;
}

export interface EtatAlerte {
  alerte: AlerteSeuil;
  evaluable: boolean;
  declenchee: boolean;
  valeurActuelle: number | null;
}
