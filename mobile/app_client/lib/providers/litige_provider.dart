import 'package:flutter_riverpod/legacy.dart';

const motifsLitige = [
  'Marchandise endommagée',
  'Retard important',
  'Colis manquant',
  'Comportement du chauffeur',
  'Facturation incorrecte',
  'Autre',
];

class LitigeState {
  final bool envoiEnCours;
  final bool envoye;

  const LitigeState({this.envoiEnCours = false, this.envoye = false});

  LitigeState copyWith({bool? envoiEnCours, bool? envoye}) {
    return LitigeState(envoiEnCours: envoiEnCours ?? this.envoiEnCours, envoye: envoye ?? this.envoye);
  }
}

/// S19 (Sprint 19, "Back-office avancé, litiges"), Volet Client — ⚠️ MOCK,
/// aucun appel réseau. service-adm (Web) n'expose aucun contrat de
/// signalement de litige à ce jour. Simule un envoi (délai court) pour
/// permettre de construire et valider l'écran dès maintenant. À remplacer
/// par un vrai POST vers service-adm une fois ce contrat exposé.
class LitigeNotifier extends StateNotifier<LitigeState> {
  LitigeNotifier() : super(const LitigeState());

  Future<void> envoyer({
    required String demandeId,
    required String missionId,
    required String motif,
    required String description,
  }) async {
    state = state.copyWith(envoiEnCours: true);
    await Future.delayed(const Duration(milliseconds: 600));
    state = state.copyWith(envoiEnCours: false, envoye: true);
  }
}

final litigeProvider = StateNotifierProvider<LitigeNotifier, LitigeState>((ref) {
  return LitigeNotifier();
});
