enum MoyenReglement { momo, orangeMoney, especes }

const libellesMoyenReglement = {
  MoyenReglement.momo: 'MTN MoMo',
  MoyenReglement.orangeMoney: 'Orange Money',
  MoyenReglement.especes: 'Espèces',
};

/// S14 (Sprint 14, "Paiements Mobile Money étendus") — ⚠️ MOCK, aucun appel
/// réseau.
///
/// `Ecriture` (grand livre miroir de service-pay, S8) ne porte aujourd'hui
/// aucun champ "moyen de règlement" — service-pay (Web) n'expose pas encore
/// cette information par écriture/mission. Cette fonction la simule de
/// façon déterministe à partir du `missionId` déjà connu, uniquement pour
/// permettre de construire et valider l'affichage dès maintenant. À
/// remplacer par un vrai champ exposé par service-pay dès qu'il sera
/// disponible — voir README.md.
MoyenReglement moyenReglementMock(String missionId) {
  final valeurs = MoyenReglement.values;
  return valeurs[missionId.hashCode.abs() % valeurs.length];
}
