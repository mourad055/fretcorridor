import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

// Intervalle de tarif (F/kg, pas pondere par le poids) parmi les chauffeurs
// actuellement actifs sur un axe+categorie (plan §1bis, PR #145 cote
// service-cap) -- l'app Client multiplie par le poids taxable de la demande
// pour afficher un prix indicatif (EstimationScreen).
class IntervalleTarif {
  final double tarifMinFcfaParKg;
  final double tarifMaxFcfaParKg;
  final int nombreChauffeurs;

  const IntervalleTarif({
    required this.tarifMinFcfaParKg,
    required this.tarifMaxFcfaParKg,
    required this.nombreChauffeurs,
  });

  factory IntervalleTarif.fromJson(Map<String, dynamic> json) => IntervalleTarif(
        tarifMinFcfaParKg: (json['tarifMinFcfaParKg'] as num).toDouble(),
        tarifMaxFcfaParKg: (json['tarifMaxFcfaParKg'] as num).toDouble(),
        nombreChauffeurs: json['nombreChauffeurs'] as int,
      );
}

class TarifIntervalleState {
  final bool chargement;
  // Distingue "pas encore de chauffeur actif sur cet axe" (ENF-DIS-04,
  // 404 cote service-cap -- pas une erreur reseau) d'une vraie panne.
  final bool introuvable;
  final String? erreur;
  final IntervalleTarif? intervalle;

  const TarifIntervalleState({this.chargement = false, this.introuvable = false, this.erreur, this.intervalle});

  TarifIntervalleState copyWith({bool? chargement, bool? introuvable, String? erreur, IntervalleTarif? intervalle}) {
    return TarifIntervalleState(
      chargement: chargement ?? this.chargement,
      introuvable: introuvable ?? false,
      erreur: erreur,
      intervalle: intervalle,
    );
  }
}

class TarifIntervalleNotifier extends StateNotifier<TarifIntervalleState> {
  final Dio _dio;

  TarifIntervalleNotifier(this._dio) : super(const TarifIntervalleState());

  Future<void> charger(String axeId, String categorieMarchandise) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/cap/tarifs/intervalle', queryParameters: {
        'axeId': axeId,
        'categorie': categorieMarchandise,
      });
      state = state.copyWith(chargement: false, intervalle: IntervalleTarif.fromJson(response.data as Map<String, dynamic>));
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) {
        state = state.copyWith(chargement: false, introuvable: true);
      } else {
        state = state.copyWith(chargement: false, erreur: 'Erreur de connexion. Vérifiez votre réseau.');
      }
    }
  }
}

final tarifIntervalleProvider = StateNotifierProvider<TarifIntervalleNotifier, TarifIntervalleState>((ref) {
  return TarifIntervalleNotifier(ref.watch(capDioProvider));
});
