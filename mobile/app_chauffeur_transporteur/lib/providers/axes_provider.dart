import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

class Axe {
  final String id;
  final String origine;
  final String destination;
  final double distanceKm;
  final bool visibiliteActive;
  final bool matchingActif;
  final bool paiementActif;

  const Axe({
    required this.id,
    required this.origine,
    required this.destination,
    required this.distanceKm,
    required this.visibiliteActive,
    required this.matchingActif,
    required this.paiementActif,
  });

  factory Axe.fromJson(Map<String, dynamic> json) => Axe(
        id: json['id'] as String,
        origine: json['origine'] as String,
        destination: json['destination'] as String,
        distanceKm: (json['distanceKm'] as num).toDouble(),
        visibiliteActive: json['visibiliteActive'] as bool,
        matchingActif: json['matchingActif'] as bool,
        paiementActif: json['paiementActif'] as bool,
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

// S3 (EF-GEO-03) : GET /axes — voir AxeController.axesMobile
// (backend/gateway/.../infrastructure/rest/geo/). Les verrous
// (matching/paiement inactifs) restent visibles, jamais masqués (RG-012).
class AxesNotifier extends StateNotifier<AxesState> {
  final Dio _dio;

  AxesNotifier(this._dio) : super(const AxesState());

  Future<void> charger() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/axes');
      state = state.copyWith(
        chargement: false,
        axes: (response.data as List<dynamic>).map((a) => Axe.fromJson(a as Map<String, dynamic>)).toList(),
      );
    } on DioException {
      state = state.copyWith(chargement: false, erreur: 'Erreur de connexion. Vérifiez votre réseau.');
    }
  }
}

final axesProvider = StateNotifierProvider<AxesNotifier, AxesState>((ref) {
  return AxesNotifier(ref.watch(dioProvider));
});
