/**
 * Coordonnées des villes du corridor CEMAC (Cameroun/Tchad) utilisées comme
 * hubs dans les données Bureau actuelles. Référentiel statique en attendant
 * les coordonnées réelles de hubs livrées par service-geo (Moteur) — cf.
 * docs/adr/0007, addendum Sprint 12. Cohérent avec les positions mockées de
 * service-trk (Douala, N'Djamena) pour éviter toute incohérence visuelle
 * entre la carte des axes et le suivi temps réel.
 */
export const VILLES_CEMAC: Record<string, [number, number]> = {
  Douala: [4.0511, 9.7679],
  Yaoundé: [3.848, 11.5021],
  Bafoussam: [5.4737, 10.4176],
  Garoua: [9.3017, 13.3921],
  Maroua: [10.591, 14.3159],
  "N'Djamena": [12.1348, 15.0557],
  Moundou: [8.5667, 16.0833],
  Sarh: [9.15, 18.3833],
};

export function coordonneesVille(nom: string): [number, number] | null {
  return VILLES_CEMAC[nom] ?? null;
}
