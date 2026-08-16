import 'package:flutter_riverpod/legacy.dart';

enum StatutProposition { enAttente, acceptee, refusee }

class PropositionRetour {
  final String id;
  final String trajetLibelle;
  final double distanceKm;
  final StatutProposition statut;

  const PropositionRetour({
    required this.id,
    required this.trajetLibelle,
    required this.distanceKm,
    required this.statut,
  });

  PropositionRetour copyWith({StatutProposition? statut}) => PropositionRetour(
        id: id,
        trajetLibelle: trajetLibelle,
        distanceKm: distanceKm,
        statut: statut ?? this.statut,
      );
}

class PropositionRetourState {
  final List<PropositionRetour> propositions;

  const PropositionRetourState({this.propositions = const []});

  List<PropositionRetour> get enAttente =>
      propositions.where((p) => p.statut == StatutProposition.enAttente).toList();
}

/// S12 (Sprint 12, "Retour à vide & replanification") — ⚠️ MOCK, aucun appel
/// réseau.
///
/// Le Moteur (service-opt) ne publie pas encore de proposition de mission
/// retour après une livraison — aucun topic Kafka `proposition-retour` (ou
/// équivalent) n'existe côté Moteur à ce jour. Ce provider simule
/// localement la réception d'une telle proposition pour permettre de
/// construire et valider l'écran (notification + acceptation/refus par le
/// chauffeur) dès maintenant. À remplacer par une vraie réception (Kafka
/// côté service-not, ou polling gateway) et un vrai POST d'acceptation/refus
/// une fois le contrat exposé côté Moteur — voir README.md.
class PropositionRetourNotifier extends StateNotifier<PropositionRetourState> {
  PropositionRetourNotifier() : super(const PropositionRetourState());

  void simulerReception() {
    if (state.propositions.any((p) => p.statut == StatutProposition.enAttente)) return;
    state = PropositionRetourState(propositions: [
      ...state.propositions,
      const PropositionRetour(
        id: 'mock-retour-s12-001',
        trajetLibelle: 'Yaoundé → Douala (retour à vide)',
        distanceKm: 245,
        statut: StatutProposition.enAttente,
      ),
    ]);
  }

  void accepter(String id) => _repondre(id, StatutProposition.acceptee);

  void refuser(String id) => _repondre(id, StatutProposition.refusee);

  void _repondre(String id, StatutProposition statut) {
    state = PropositionRetourState(
      propositions: [
        for (final p in state.propositions)
          if (p.id == id) p.copyWith(statut: statut) else p,
      ],
    );
  }
}

final propositionRetourProvider = StateNotifierProvider<PropositionRetourNotifier, PropositionRetourState>((ref) {
  return PropositionRetourNotifier();
});
