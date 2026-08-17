class AxeMock {
  final String id;
  final String origine;
  final String destination;

  const AxeMock({required this.id, required this.origine, required this.destination});
}

/// S15 (Sprint 15, "Second axe & sécurité") — ⚠️ MOCK, aucun appel réseau.
///
/// Aucun endpoint `/axes` n'est exposé côté app Client aujourd'hui
/// (service-geo n'est branché que côté app Chauffeur/Transporteur, S3) —
/// et aucun écran/provider "axe" n'existait ici avant ce sprint. Cette
/// liste simule les axes disponibles pour permettre de construire et
/// valider le sélecteur au moment de la demande d'envoi. À remplacer par
/// un vrai fetch (probablement le même `/axes` que côté Chauffeur, ou un
/// équivalent Client) dès que service-geo l'exposera pour ce module —
/// voir README.md.
const axesMockDisponibles = [
  AxeMock(id: 'mock-axe-client-001', origine: 'Yaoundé', destination: 'Douala'),
  AxeMock(id: 'mock-axe-client-002', origine: 'Douala', destination: 'Bafoussam'),
];
