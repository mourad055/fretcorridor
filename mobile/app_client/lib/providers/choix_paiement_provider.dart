import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

enum MoyenPaiement { momo, orangeMoney, especes }

const libellesMoyenPaiement = {
  MoyenPaiement.momo: 'MTN MoMo',
  MoyenPaiement.orangeMoney: 'Orange Money',
  MoyenPaiement.especes: 'Espèces',
};

// S14 (EF-PAY-06/07) : MoMo et Orange Money envoient tous deux
// MONNAIE_ELECTRONIQUE côté service-pay (ModePaiement) — le backend ne
// distingue pas plus finement entre prestataires. Espèces n'a
// volontairement aucune entrée ici : hors périmètre du choix a priori
// (mode dégradé décidé à l'enlèvement par le chauffeur, jamais planifié en
// amont côté app — cf. commit service-pay Item B), reste confirmé
// localement sans appel réseau.
const _modePaiementBackend = {
  MoyenPaiement.momo: 'MONNAIE_ELECTRONIQUE',
  MoyenPaiement.orangeMoney: 'MONNAIE_ELECTRONIQUE',
};

class ChoixPaiementState {
  final MoyenPaiement? moyenSelectionne;
  final bool confirme;
  final bool chargement;
  final String? erreur;

  const ChoixPaiementState({this.moyenSelectionne, this.confirme = false, this.chargement = false, this.erreur});

  ChoixPaiementState copyWith({MoyenPaiement? moyenSelectionne, bool? confirme, bool? chargement, String? erreur}) =>
      ChoixPaiementState(
        moyenSelectionne: moyenSelectionne ?? this.moyenSelectionne,
        confirme: confirme ?? this.confirme,
        chargement: chargement ?? this.chargement,
        erreur: erreur,
      );
}

/// S14 (Sprint 14, "Paiements Mobile Money étendus") : appelle réellement
/// `POST /missions/{id}/moyen-paiement` côté service-pay — pas de gateway
/// unifiée pour l'app Client, appel direct (même principe que les autres
/// providers de ce dépôt, cf. dio_provider.dart).
class ChoixPaiementNotifier extends StateNotifier<ChoixPaiementState> {
  final Dio _dio;

  ChoixPaiementNotifier(this._dio) : super(const ChoixPaiementState());

  void selectionner(MoyenPaiement moyen) {
    state = ChoixPaiementState(moyenSelectionne: moyen, confirme: false);
  }

  Future<void> confirmer(String missionId, String? tenantId) async {
    final moyen = state.moyenSelectionne;
    if (moyen == null) return;

    final modePaiement = _modePaiementBackend[moyen];
    if (modePaiement == null) {
      // Espèces : confirmation purement locale (voir commentaire ci-dessus).
      state = state.copyWith(confirme: true, erreur: null);
      return;
    }
    if (tenantId == null) {
      state = state.copyWith(erreur: 'Session incomplète — reconnectez-vous et réessayez.');
      return;
    }

    state = state.copyWith(chargement: true, erreur: null);
    try {
      await _dio.post('/missions/$missionId/moyen-paiement', data: {
        'tenantId': tenantId,
        'modePaiement': modePaiement,
      });
      state = state.copyWith(chargement: false, confirme: true);
    } on DioException {
      state = state.copyWith(chargement: false, erreur: 'Impossible d\'enregistrer votre choix. Réessayez.');
    }
  }
}

final choixPaiementProvider = StateNotifierProvider<ChoixPaiementNotifier, ChoixPaiementState>((ref) {
  return ChoixPaiementNotifier(ref.watch(payDioProvider));
});
