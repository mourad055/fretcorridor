import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

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
  final String? erreur;

  const LitigeState({this.envoiEnCours = false, this.envoye = false, this.erreur});

  LitigeState copyWith({bool? envoiEnCours, bool? envoye, String? erreur}) {
    return LitigeState(
      envoiEnCours: envoiEnCours ?? this.envoiEnCours,
      envoye: envoye ?? this.envoye,
      erreur: erreur,
    );
  }
}

/// S19 (Sprint 19, "Back-office avancé, litiges"), Volet Client — appel réel
/// vers service-adm (POST /api/v1/dossiers, type LITIGE) depuis le 23 août.
/// Contrat étendu côté backend (audit de suivi) pour porter motif/description
/// en clair, absents jusqu'ici (pensé pour un dossier ouvert côté ADM, pas
/// pour la plainte initiale d'un chargeur) ; le délai de traitement n'est pas
/// envoyé (aucun sens côté chargeur) — DossierController applique un délai
/// par défaut (72h, hypothèse d'équipe documentée côté backend).
class LitigeNotifier extends StateNotifier<LitigeState> {
  final Dio _dio;

  LitigeNotifier(this._dio) : super(const LitigeState());

  Future<void> envoyer({
    required String demandeId,
    required String missionId,
    required String motif,
    required String description,
  }) async {
    state = state.copyWith(envoiEnCours: true, erreur: null);
    try {
      await _dio.post('/dossiers', data: {
        'type': 'LITIGE',
        'priorite': 'NORMALE',
        'missionId': missionId,
        'motif': motif,
        'description': description,
      });
      state = state.copyWith(envoiEnCours: false, envoye: true);
    } on DioException catch (_) {
      state = state.copyWith(
        envoiEnCours: false,
        erreur: 'Impossible d\'envoyer le signalement. Vérifiez votre connexion et réessayez.',
      );
    }
  }
}

final litigeProvider = StateNotifierProvider<LitigeNotifier, LitigeState>((ref) {
  return LitigeNotifier(ref.watch(admDioProvider));
});
