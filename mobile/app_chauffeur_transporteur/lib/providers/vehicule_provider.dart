import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

class VehiculeFlotte {
  final String id;
  final String typeVehicule;
  final String? immatriculation;
  final double? profilPoidsMaxTonnes;
  final int? profilNombreEssieux;
  final bool profilMatieresDangereuses;

  const VehiculeFlotte({
    required this.id,
    required this.typeVehicule,
    this.immatriculation,
    this.profilPoidsMaxTonnes,
    this.profilNombreEssieux,
    this.profilMatieresDangereuses = false,
  });

  factory VehiculeFlotte.fromJson(Map<String, dynamic> json) => VehiculeFlotte(
        id: json['id'] as String,
        typeVehicule: json['typeVehicule'] as String,
        immatriculation: json['immatriculation'] as String?,
        profilPoidsMaxTonnes: (json['profilPoidsMaxTonnes'] as num?)?.toDouble(),
        profilNombreEssieux: json['profilNombreEssieux'] as int?,
        profilMatieresDangereuses: json['profilMatieresDangereuses'] as bool? ?? false,
      );
}

class VehiculeState {
  final bool chargement;
  final String? erreur;
  final List<VehiculeFlotte> vehicules;

  const VehiculeState({this.chargement = false, this.erreur, this.vehicules = const []});

  VehiculeState copyWith({bool? chargement, String? erreur, List<VehiculeFlotte>? vehicules}) {
    return VehiculeState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      vehicules: vehicules ?? this.vehicules,
    );
  }
}

// S10 : console de flotte simplifiée — POST/GET /vehicules, voir
// VehiculeController (backend/gateway/.../infrastructure/rest/flt/). Ferme
// le TODO du S4 : la déclaration de capacité utilise désormais un vehiculeId
// réel plutôt qu'un identifiant généré localement sur l'appareil.
class VehiculeNotifier extends StateNotifier<VehiculeState> {
  final Dio _dio;

  VehiculeNotifier(this._dio) : super(const VehiculeState());

  Future<void> chargerMesVehicules() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/vehicules/mes');
      state = state.copyWith(
        chargement: false,
        vehicules: (response.data as List<dynamic>).map((v) => VehiculeFlotte.fromJson(v as Map<String, dynamic>)).toList(),
      );
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
    }
  }

  Future<bool> declarer({
    required String typeVehicule,
    String? immatriculation,
    double? profilPoidsMaxTonnes,
    int? profilNombreEssieux,
    bool profilMatieresDangereuses = false,
  }) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.post('/vehicules', data: {
        'typeVehicule': typeVehicule,
        'immatriculation': immatriculation,
        'profilPoidsMaxTonnes': profilPoidsMaxTonnes,
        'profilNombreEssieux': profilNombreEssieux,
        'profilMatieresDangereuses': profilMatieresDangereuses,
      });
      final nouveau = VehiculeFlotte.fromJson(response.data);
      state = state.copyWith(chargement: false, vehicules: [nouveau, ...state.vehicules]);
      return true;
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
      return false;
    }
  }

  String _messageErreur(DioException e) {
    if (e.response?.statusCode == 400) {
      return (e.response?.data is Map ? e.response?.data['detail'] as String? : null) ?? 'Requête refusée.';
    }
    if (e.response?.statusCode == 503) return 'Service de flotte momentanément indisponible.';
    return 'Erreur de connexion. Vérifiez votre réseau.';
  }
}

final vehiculeProvider = StateNotifierProvider<VehiculeNotifier, VehiculeState>((ref) {
  return VehiculeNotifier(ref.watch(dioProvider));
});
