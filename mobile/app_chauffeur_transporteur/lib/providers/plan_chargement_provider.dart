import 'package:flutter_riverpod/legacy.dart';

enum OrientationColis { debout, couche, fragileDessus }

class ColisPlan {
  final String id;
  final String libelle;
  final double poidsKg;
  final OrientationColis orientation;
  final int position; // ordre avant → arrière dans le véhicule

  const ColisPlan({
    required this.id,
    required this.libelle,
    required this.poidsKg,
    required this.orientation,
    required this.position,
  });
}

class EssieuCharge {
  final String libelle;
  final double poidsKg;
  final double poidsMaxKg;

  const EssieuCharge({required this.libelle, required this.poidsKg, required this.poidsMaxKg});

  bool get surcharge => poidsKg > poidsMaxKg;
}

class PlanChargement {
  final String missionId;
  final String? etapeLibelle;
  final List<ColisPlan> colis;
  final List<EssieuCharge> essieux;

  const PlanChargement({required this.missionId, this.etapeLibelle, required this.colis, required this.essieux});
}

class PlanChargementState {
  final PlanChargement? plan;

  const PlanChargementState({this.plan});
}

/// S16 (Sprint 16, "Oracle de chargement 3D") — ⚠️ MOCK, aucun appel réseau.
///
/// service-opt V2 (Moteur) n'expose pas encore l'oracle de chargement 3D
/// (positions des colis, orientations, répartition des charges par
/// essieu) — aucun contrat backend à ce jour. Restitution purement visuelle
/// et en lecture seule pour le chauffeur : c'est le Moteur qui calcule, pas
/// l'app. Ce provider simule un plan de chargement plausible pour permettre
/// de construire et valider l'écran dès maintenant. À remplacer par un vrai
/// GET (contrat à définir avec le Moteur) une fois service-opt V2 prêt.
class PlanChargementNotifier extends StateNotifier<PlanChargementState> {
  PlanChargementNotifier() : super(const PlanChargementState());

  void chargerPlanDemo(String missionId, {String? etapeLibelle}) {
    state = PlanChargementState(
      plan: PlanChargement(
        missionId: missionId,
        etapeLibelle: etapeLibelle,
        colis: const [
          ColisPlan(id: 'c1', libelle: 'Sacs de ciment (Palette A)', poidsKg: 850, orientation: OrientationColis.debout, position: 1),
          ColisPlan(id: 'c2', libelle: 'Caisses électroménager (Palette B)', poidsKg: 620, orientation: OrientationColis.fragileDessus, position: 2),
          ColisPlan(id: 'c3', libelle: 'Rouleaux de tissus', poidsKg: 430, orientation: OrientationColis.couche, position: 3),
          ColisPlan(id: 'c4', libelle: 'Bidons d\'huile végétale', poidsKg: 980, orientation: OrientationColis.debout, position: 4),
        ],
        essieux: const [
          EssieuCharge(libelle: 'Essieu avant', poidsKg: 3200, poidsMaxKg: 6000),
          EssieuCharge(libelle: 'Essieu arrière', poidsKg: 5800, poidsMaxKg: 13000),
        ],
      ),
    );
  }
}

final planChargementProvider = StateNotifierProvider<PlanChargementNotifier, PlanChargementState>((ref) {
  return PlanChargementNotifier();
});
