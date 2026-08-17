import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'auth_provider.dart';
import 'dio_provider.dart';

class Axe {
  final String id;
  final String origine;
  final String destination;

  const Axe({required this.id, required this.origine, required this.destination});

  factory Axe.fromJson(Map<String, dynamic> json) => Axe(
        id: json['id'] as String,
        origine: json['hubOrigineNom'] as String,
        destination: json['hubDestinationNom'] as String,
      );
}

class AxesState {
  final bool chargement;
  final String? erreur;
  final List<Axe> axes;

  const AxesState({this.chargement = false, this.erreur, this.axes = const []});

  AxesState copyWith({bool? chargement, String? erreur, List<Axe>? axes}) {
    return AxesState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      axes: axes ?? this.axes,
    );
  }
}

// S15 (Sprint 15, "Second axe & sécurité"), Volet Client — sélecteur d'axe
// facultatif à la publication d'une demande. GET /api/geo/axes?tenantId=
// filtre réellement par tenant en base côté service-geo (ENF-MUL-01),
// confirmé par le Moteur — voir geoDioProvider.
class AxesNotifier extends StateNotifier<AxesState> {
  final Dio _dio;
  final String? _tenantId;

  AxesNotifier(this._dio, this._tenantId) : super(const AxesState());

  Future<void> charger() async {
    if (_tenantId == null) return;
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/api/geo/axes', queryParameters: {'tenantId': _tenantId});
      state = state.copyWith(
        chargement: false,
        axes: (response.data as List<dynamic>).map((a) => Axe.fromJson(a as Map<String, dynamic>)).toList(),
      );
    } on DioException {
      state = state.copyWith(chargement: false, erreur: 'Impossible de charger les axes.');
    }
  }
}

final axesProvider = StateNotifierProvider<AxesNotifier, AxesState>((ref) {
  final tenantId = ref.watch(authProvider).tenantId;
  return AxesNotifier(ref.watch(geoDioProvider), tenantId);
});
