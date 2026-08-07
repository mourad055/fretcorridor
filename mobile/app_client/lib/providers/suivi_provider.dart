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
  final Dio _dio;
  SuiviNotifier(this._dio) : super(const SuiviState());

  // S7 puis S6 : la chronologie donne le missionId, nécessaire pour la position.
  // Tant qu'aucune mission n'existe encore pour cette demande (matching pas
  // encore actif — S5 est un stub), les deux resteront simplement vides.
  Future<void> charger(String demandeId) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/missions/demande/$demandeId/chronologie');
      if (response.statusCode == 204 || response.data == null) {
        state = state.copyWith(chargement: false, chronologie: null, position: null);
        return;
      }
      final chronologie = ChronologieModel.fromJson(response.data);
      state = state.copyWith(chargement: false, chronologie: chronologie);
      await _chargerPosition(chronologie.missionId);
    } on DioException catch (e) {
      if (e.response?.statusCode == 204) {
        state = state.copyWith(chargement: false, chronologie: null);
      } else {
        state = state.copyWith(chargement: false, erreur: 'Impossible de charger le suivi.');
      }
    }
  }

  Future<void> _chargerPosition(String missionId) async {
    try {
      final response = await _dio.get('/positions/mission/$missionId/derniere');
      if (response.statusCode == 200 && response.data != null) {
        state = state.copyWith(position: PositionModel.fromJson(response.data));
      }
    } on DioException {
      // Pas de position disponible — état normal tant que le chauffeur n'en envoie pas
    }
  }
}

final suiviProvider = StateNotifierProvider<SuiviNotifier, SuiviState>((ref) {
  return SuiviNotifier(ref.watch(dioProvider));
});
