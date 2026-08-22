import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'package:geolocator/geolocator.dart';
import 'dio_provider.dart';
import 'vehicule_provider.dart';

class CapaciteDeclaree {
  final String id;
  final String axeId;
  final String modeDeclaration;
  final double poidsKg;
  final double poidsTaxableKg;
  final bool publiee;
  final bool expiree;
  final DateTime? dateDepart;
  final DateTime? dateCreation;
  final String? vehiculeId;

  const CapaciteDeclaree({
    required this.id,
    required this.axeId,
    required this.modeDeclaration,
    required this.poidsKg,
    required this.poidsTaxableKg,
    required this.publiee,
    this.expiree = false,
    this.dateDepart,
    this.dateCreation,
    this.vehiculeId,
  });

  factory CapaciteDeclaree.fromJson(Map<String, dynamic> json) => CapaciteDeclaree(
        id: json['id'] as String,
        axeId: json['axeId'] as String,
        modeDeclaration: json['modeDeclaration'] as String,
        poidsKg: (json['poidsKg'] as num).toDouble(),
        poidsTaxableKg: (json['poidsTaxableKg'] as num).toDouble(),
        publiee: json['publiee'] as bool,
        expiree: json['expiree'] as bool? ?? false,
        dateDepart: json['dateDepart'] != null ? DateTime.tryParse(json['dateDepart'] as String)?.toLocal() : null,
        dateCreation: json['dateCreation'] != null ? DateTime.tryParse(json['dateCreation'] as String)?.toLocal() : null,
        vehiculeId: json['vehiculeId'] as String?,
      );
}

class CapaciteState {
  final bool chargement;
  final String? erreur;
  final CapaciteDeclaree? derniereCapacite;
  final List<CapaciteDeclaree> mesCapacites;

  const CapaciteState({this.chargement = false, this.erreur, this.derniereCapacite, this.mesCapacites = const []});

  CapaciteState copyWith({bool? chargement, String? erreur, CapaciteDeclaree? derniereCapacite, List<CapaciteDeclaree>? mesCapacites}) {
    return CapaciteState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      derniereCapacite: derniereCapacite ?? this.derniereCapacite,
      mesCapacites: mesCapacites ?? this.mesCapacites,
    );
  }
}

// S4 (EF-CAP-03/07) : POST /capacites — voir CapaciteDeclarationController
// (backend/gateway/.../infrastructure/rest/cap/). vehiculeId vient désormais
// du registre réel de la flotte (S10, VehiculeFlotte) plutôt que d'un
// identifiant généré localement sur l'appareil — TODO du S4 fermé.
class CapaciteNotifier extends StateNotifier<CapaciteState> {
  final Dio _dio;

  CapaciteNotifier(this._dio) : super(const CapaciteState());

  Future<void> declarer({
    required VehiculeFlotte vehicule,
    required String axeId,
    required double poidsKg,
    required DateTime dateDepart,
  }) async {
    state = state.copyWith(chargement: true, erreur: null);

    final position = await _positionActuelle();
    if (position == null) {
      state = state.copyWith(chargement: false, erreur: 'Position GPS indisponible — activez la localisation.');
      return;
    }

    try {
      final response = await _dio.post('/capacites', data: {
        'vehiculeId': vehicule.id,
        'axeId': axeId,
        'modeDeclaration': 'TOTALE',
        'poidsKg': poidsKg,
        'origineLatitude': position.latitude,
        'origineLongitude': position.longitude,
        'typeVehicule': vehicule.typeVehicule,
        'profilPoidsMaxTonnes': vehicule.profilPoidsMaxTonnes,
        'profilNombreEssieux': vehicule.profilNombreEssieux,
        'profilMatieresDangereuses': vehicule.profilMatieresDangereuses,
        'dateDepart': dateDepart.toUtc().toIso8601String(),
      });
      state = state.copyWith(chargement: false, derniereCapacite: CapaciteDeclaree.fromJson(response.data));
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
    }
  }

  Future<void> chargerMesCapacites() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/capacites/mes');
      final liste = (response.data as List).map((e) => CapaciteDeclaree.fromJson(e)).toList();
      state = state.copyWith(chargement: false, mesCapacites: liste);
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
    }
  }

  Future<bool> supprimer(String capaciteId) async {
    try {
      await _dio.delete('/capacites/$capaciteId');
      state = state.copyWith(mesCapacites: state.mesCapacites.where((c) => c.id != capaciteId).toList());
      return true;
    } on DioException catch (e) {
      state = state.copyWith(erreur: _messageErreur(e));
      return false;
    }
  }

  Future<Position?> _positionActuelle() async {
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied || permission == LocationPermission.deniedForever) {
      return null;
    }
    try {
      return await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(accuracy: LocationAccuracy.medium),
      );
    } catch (_) {
      return null;
    }
  }

  String _messageErreur(DioException e) {
    final status = e.response?.statusCode;
    if (status == 400) {
      return (e.response?.data is Map ? e.response?.data['detail'] as String? : null) ?? 'Requête refusée.';
    }
    if (status == 503) return 'Service de capacité momentanément indisponible.';
    return 'Erreur de connexion. Vérifiez votre réseau.';
  }
}

final capaciteProvider = StateNotifierProvider<CapaciteNotifier, CapaciteState>((ref) {
  return CapaciteNotifier(ref.watch(dioProvider));
});
