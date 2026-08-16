import '../providers/axes_provider.dart';

/// S15 (Sprint 15, "Second axe & sécurité") — ⚠️ MOCK, aucun appel réseau.
///
/// service-geo (Moteur) n'expose aujourd'hui qu'un seul axe réel par tenant
/// (`GET /axes`, S3). Cet axe fictif permet de construire et valider le
/// sélecteur multi-axes dès maintenant, sans attendre que le Moteur expose
/// plusieurs axes actifs. Marqué visuellement comme démonstration dans
/// axes_screen.dart (jamais confondu avec un axe réel) — à supprimer dès
/// que `/axes` renvoie plusieurs axes en conditions réelles.
const axeMockSecondaire = Axe(
  id: 'mock-axe-s15-001',
  origine: 'Douala',
  destination: 'Bafoussam',
  distanceKm: 290,
  visibiliteActive: true,
  matchingActif: false,
  paiementActif: false,
);
