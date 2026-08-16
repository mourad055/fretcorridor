import 'package:flutter_riverpod/legacy.dart';

enum MoyenPaiement { momo, orangeMoney, especes }

const libellesMoyenPaiement = {
  MoyenPaiement.momo: 'MTN MoMo',
  MoyenPaiement.orangeMoney: 'Orange Money',
  MoyenPaiement.especes: 'Espèces',
};

class ChoixPaiementState {
  final MoyenPaiement? moyenSelectionne;
  final bool confirme;

  const ChoixPaiementState({this.moyenSelectionne, this.confirme = false});

  ChoixPaiementState copyWith({MoyenPaiement? moyenSelectionne, bool? confirme}) => ChoixPaiementState(
        moyenSelectionne: moyenSelectionne ?? this.moyenSelectionne,
        confirme: confirme ?? this.confirme,
      );
}

/// S14 (Sprint 14, "Paiements Mobile Money étendus") — ⚠️ MOCK, aucun appel
/// réseau.
///
/// service-pay (Web) n'expose pas encore d'intégration réelle de paiement
/// côté client (le prestataire Mobile Money agréé n'est pas encore branché).
/// Ce provider simule localement le choix d'un moyen de règlement et sa
/// confirmation, pour permettre de construire et valider l'écran dès
/// maintenant. À remplacer par un vrai appel service-pay (initiation de
/// paiement) une fois le prestataire intégré — voir README.md.
class ChoixPaiementNotifier extends StateNotifier<ChoixPaiementState> {
  ChoixPaiementNotifier() : super(const ChoixPaiementState());

  void selectionner(MoyenPaiement moyen) {
    state = ChoixPaiementState(moyenSelectionne: moyen, confirme: false);
  }

  void confirmerMock() {
    if (state.moyenSelectionne == null) return;
    state = state.copyWith(confirme: true);
  }
}

final choixPaiementProvider = StateNotifierProvider<ChoixPaiementNotifier, ChoixPaiementState>((ref) {
  return ChoixPaiementNotifier();
});
