export interface Position {
  id: string;
  vehiculeLabel: string;
  latitude: number;
  longitude: number;
  capturedLe: string;
  ageSecondes: number;
}

/**
 * RG-043 : toute position affichée porte un âge lisible — jamais un
 * horodatage brut seul. Fonction pure, testée indépendamment du composant.
 */
export function formatAge(ageSecondes: number): string {
  if (ageSecondes < 60) {
    return "à l'instant";
  }
  if (ageSecondes < 3600) {
    const minutes = Math.floor(ageSecondes / 60);
    return `il y a ${minutes} min`;
  }
  const heures = Math.floor(ageSecondes / 3600);
  return `il y a ${heures} h`;
}
