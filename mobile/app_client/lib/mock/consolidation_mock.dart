import '../models/chronologie_model.dart';

/// S11 (Sprint 11, "Consolidation LTL, moteur V1") — ⚠️ MOCK, aucun appel
/// réseau, aucune donnée serveur.
///
/// Ni `ChronologieModel` (service-exe) ni aucun autre contrat backend
/// n'expose aujourd'hui si un envoi fait partie d'une tournée de groupage
/// consolidée avec d'autres envois. Cette fonction simule cette information
/// de façon déterministe à partir du `missionId` déjà connu, uniquement pour
/// permettre de construire et valider l'indicateur visuel côté écran de
/// suivi. À remplacer par un vrai champ exposé par la chronologie (ou un
/// endpoint dédié) dès que le backend le permettra — voir README.md.
///
/// Cas par défaut inchangé : tant qu'aucune chronologie n'existe (avant
/// prise en charge), cette fonction n'est pas appelée et rien ne s'affiche.
bool estEnvoiConsolideMock(ChronologieModel chronologie) {
  return chronologie.missionId.hashCode.isEven;
}
