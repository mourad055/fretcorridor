import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

class EssieuCharge {
  final String libelle;
  final double poidsKg;

  const EssieuCharge({required this.libelle, required this.poidsKg});
}

class EtapePlanChargement {
  final int rang;
  final String typeEtape;
  final String demandeId;
  final List<EssieuCharge> essieux;

  const EtapePlanChargement({
    required this.rang,
    required this.typeEtape,
    required this.demandeId,
    required this.essieux,
  });
}

class PlanChargementState {
  final bool chargement;
  final String? erreur;
  final List<EtapePlanChargement> etapes;

  const PlanChargementState({this.chargement = false, this.erreur, this.etapes = const []});

  PlanChargementState copyWith({bool? chargement, String? erreur, List<EtapePlanChargement>? etapes}) {
    return PlanChargementState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      etapes: etapes ?? this.etapes,
    );
  }
}

/// S16/EF-MAT-13 (audit de suivi, 23 août) — appel réel depuis ce correctif :
/// GET /missions/tournees/{tourneeId} (même endpoint que mission_multi_etapes,
/// désormais enrichi côté service-exe avec chargesParEssieu par étape,
/// alimenté par PlanChargeConfirme, service-opt).
///
/// Ne restitue QUE la répartition de poids par essieu (approximation
/// uniforme, cf javadoc backend OracleChargementService) — les positions et
/// orientations réelles des colis dans le véhicule ne sont PAS calculées par
/// le Moteur (le contrat colis 3D n'existe pas encore) et ne sont donc
/// jamais affichées ici, contrairement à l'ancienne maquette qui les
/// inventait pour la démonstration.
///
/// Une étape sans chargesParEssieu (tournée pas encore confirmée par
/// l'oracle, ou événement pas encore reçu) est simplement absente de la
/// liste plutôt que remplie d'une valeur inventée.
class PlanChargementNotifier extends StateNotifier<PlanChargementState> {
  final Dio _dio;

  PlanChargementNotifier(this._dio) : super(const PlanChargementState());

  Future<void> charger(String tourneeId, {String? missionIdFiltre}) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/missions/tournees/$tourneeId');
      final etapesJson = response.data['etapes'] as List<dynamic>? ?? [];
      final etapes = etapesJson
          .where((e) => missionIdFiltre == null || e['missionId'] == missionIdFiltre)
          .where((e) => e['chargesParEssieu'] != null)
          .map((e) {
        final charges = (e['chargesParEssieu'] as Map<String, dynamic>);
        final essieux = charges.entries
            .map((entree) => EssieuCharge(
                  libelle: _libelleEssieu(entree.key),
                  poidsKg: (entree.value as num).toDouble(),
                ))
            .toList()
          ..sort((a, b) => a.libelle.compareTo(b.libelle));
        return EtapePlanChargement(
          rang: e['rang'] as int,
          typeEtape: e['typeEtape'] as String,
          demandeId: e['demandeId'] as String,
          essieux: essieux,
        );
      }).toList()
        ..sort((a, b) => a.rang.compareTo(b.rang));

      state = state.copyWith(chargement: false, etapes: etapes);
    } on DioException catch (_) {
      state = state.copyWith(
        chargement: false,
        erreur: 'Impossible de charger le plan de chargement. Vérifiez votre connexion et réessayez.',
      );
    }
  }

  String _libelleEssieu(String cle) {
    final numero = cle.replaceAll(RegExp(r'[^0-9]'), '');
    return numero.isEmpty ? cle : 'Essieu $numero';
  }
}

final planChargementProvider = StateNotifierProvider<PlanChargementNotifier, PlanChargementState>((ref) {
  return PlanChargementNotifier(ref.watch(dioProvider));
});
