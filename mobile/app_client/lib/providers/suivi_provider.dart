import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import '../models/chronologie_model.dart';
import 'dio_provider.dart';

class SuiviState {
  final bool chargement;
  final ChronologieModel? chronologie;
  final PositionModel? position;
  final String? erreur;

  const SuiviState({this.chargement = false, this.chronologie, this.position, this.erreur});

  SuiviState copyWith({bool? chargement, ChronologieModel? chronologie, PositionModel? position, String? erreur}) {
    return SuiviState(
      chargement: chargement ?? this.chargement,
      chronologie: chronologie ?? this.chronologie,
      position: position ?? this.position,
      erreur: erreur,
    );
  }
}

class SuiviNotifier extends StateNotifier<SuiviState> {
  // La chronologie (service-exe) et la dernière position (service-flt) sont
  // deux microservices distincts, chacun avec son propre client.
  final Dio _dioExe;
  final Dio _dioFlt;
  SuiviNotifier(this._dioExe, this._dioFlt) : super(const SuiviState());

  // S7 puis S6 : la chronologie donne le missionId, nécessaire pour la position.
  // Tant qu'aucune mission n'existe encore pour cette demande (matching pas
  // encore actif — S5 est un stub), les deux resteront simplement vides.
  //
  // BUG CORRIGE : copyWith(chronologie: null, ...) ne vide jamais le champ —
  // `null ?? this.chronologie` retombe sur l'ancienne valeur (copyWith ne
  // distingue pas "non fourni" de "explicitement null"). Résultat : ouvrir
  // le suivi d'une demande sans mission encore affichait le suivi de la
  // DERNIÈRE demande consultée qui, elle, en avait un. État repartI à zéro
  // ici (nouvel objet, pas copyWith) au tout début de chaque chargement.
  Future<void> charger(String demandeId) async {
    state = const SuiviState(chargement: true);
    try {
      final response = await _dioExe.get('/missions/demande/$demandeId/chronologie');
      if (response.statusCode == 204 || response.data == null) {
        state = const SuiviState(chargement: false);
        return;
      }
      final chronologie = ChronologieModel.fromJson(response.data);
      state = state.copyWith(chargement: false, chronologie: chronologie);
      await _chargerPosition(chronologie.missionId);
    } on DioException catch (e) {
      if (e.response?.statusCode == 204) {
        state = const SuiviState(chargement: false);
      } else {
        state = SuiviState(chargement: false, erreur: 'Impossible de charger le suivi.');
      }
    }
  }

  Future<void> _chargerPosition(String missionId) async {
    try {
      final response = await _dioFlt.get('/positions/mission/$missionId/derniere');
      if (response.statusCode == 200 && response.data != null) {
        state = state.copyWith(position: PositionModel.fromJson(response.data));
      }
    } on DioException {
      // Pas de position disponible — état normal tant que le chauffeur n'en envoie pas
    }
  }
}

final suiviProvider = StateNotifierProvider<SuiviNotifier, SuiviState>((ref) {
  return SuiviNotifier(ref.watch(exeDioProvider), ref.watch(fltDioProvider));
});
