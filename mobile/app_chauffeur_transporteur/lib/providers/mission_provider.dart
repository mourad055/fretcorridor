import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

class Mission {
  final String missionId;
  final String statut;
  final String? origineNom;
  final String? destinationNom;
  final String? dateCreation;
  final String? tourneeId;

  const Mission({
    required this.missionId,
    required this.statut,
    this.origineNom,
    this.destinationNom,
    this.dateCreation,
    this.tourneeId,
  });

  factory Mission.fromJson(Map<String, dynamic> json) => Mission(
        missionId: json['missionId'] as String,
        statut: json['statut'] as String,
        origineNom: json['origineNom'] as String?,
        destinationNom: json['destinationNom'] as String?,
        dateCreation: json['dateCreation'] as String?,
        tourneeId: json['tourneeId'] as String?,
      );
}

class Etape {
  final String type;
  final String libelle;
  final String? horodatageCapture;
  final String? horodatageTransmission;

  const Etape({required this.type, required this.libelle, this.horodatageCapture, this.horodatageTransmission});

  factory Etape.fromJson(Map<String, dynamic> json) => Etape(
        type: json['type'] as String,
        libelle: json['libelle'] as String,
        horodatageCapture: json['horodatageCapture'] as String?,
        horodatageTransmission: json['horodatageTransmission'] as String?,
      );
}

class MissionDetail {
  final String missionId;
  final String statut;
  final List<Etape> etapes;

  const MissionDetail({required this.missionId, required this.statut, this.etapes = const []});

  factory MissionDetail.fromJson(Map<String, dynamic> json) => MissionDetail(
        missionId: json['missionId'] as String,
        statut: json['statut'] as String,
        etapes: (json['etapes'] as List<dynamic>? ?? []).map((e) => Etape.fromJson(e as Map<String, dynamic>)).toList(),
      );
}

class MissionState {
  final bool chargement;
  final String? erreur;
  final List<Mission> missions;
  final MissionDetail? detail;

  const MissionState({this.chargement = false, this.erreur, this.missions = const [], this.detail});

  MissionState copyWith({bool? chargement, String? erreur, List<Mission>? missions, MissionDetail? detail}) {
    return MissionState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      missions: missions ?? this.missions,
      detail: detail ?? this.detail,
    );
  }
}

// S7 (EF-EXE-02/04) : GET /missions/mes, GET /missions/{id}, POST
// /missions/{id}/etapes — voir MissionExecutionController (gateway).
// Peut rester vide en pratique tant que transporteurId n'est pas peuplé en
// amont côté service-opt (écart documenté, cf. README).
class MissionNotifier extends StateNotifier<MissionState> {
  final Dio _dio;

  MissionNotifier(this._dio) : super(const MissionState());

  Future<void> chargerMesMissions() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/missions/mes');
      state = state.copyWith(
        chargement: false,
        missions: (response.data as List<dynamic>).map((m) => Mission.fromJson(m as Map<String, dynamic>)).toList(),
      );
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
    }
  }

  Future<void> chargerDetail(String missionId) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/missions/$missionId');
      state = state.copyWith(chargement: false, detail: MissionDetail.fromJson(response.data));
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
    }
  }

  Future<bool> ajouterEtape(String missionId, String type, String libelle) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.post('/missions/$missionId/etapes', data: {
        'type': type,
        'libelle': libelle,
      });
      state = state.copyWith(chargement: false, detail: MissionDetail.fromJson(response.data));
      return true;
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
      return false;
    }
  }

  String _messageErreur(DioException e) {
    final status = e.response?.statusCode;
    if (status == 404) return 'Mission introuvable.';
    if (status == 400) {
      return (e.response?.data is Map ? e.response?.data['detail'] as String? : null) ?? 'Requête refusée.';
    }
    if (status == 503) return 'Service d\'exécution momentanément indisponible.';
    return 'Erreur de connexion. Vérifiez votre réseau.';
  }
}

final missionProvider = StateNotifierProvider<MissionNotifier, MissionState>((ref) {
  return MissionNotifier(ref.watch(dioProvider));
});
