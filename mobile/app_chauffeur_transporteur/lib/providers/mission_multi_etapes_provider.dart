import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

enum TypeEtapeTournee { enlevement, livraison }

TypeEtapeTournee _typeEtapeDe(String valeur) =>
    valeur == 'LIVRAISON' ? TypeEtapeTournee.livraison : TypeEtapeTournee.enlevement;

class EtapeTournee {
  final String missionId;
  final int rang;
  final TypeEtapeTournee type;
  final String demandeId;
  final double pointLatitude;
  final double pointLongitude;
  final String? missionStatut;

  const EtapeTournee({
    required this.missionId,
    required this.rang,
    required this.type,
    required this.demandeId,
    required this.pointLatitude,
    required this.pointLongitude,
    this.missionStatut,
  });

  factory EtapeTournee.fromJson(Map<String, dynamic> json) => EtapeTournee(
        missionId: json['missionId'] as String,
        rang: json['rang'] as int,
        type: _typeEtapeDe(json['typeEtape'] as String),
        demandeId: json['demandeId'] as String,
        pointLatitude: (json['pointLatitude'] as num).toDouble(),
        pointLongitude: (json['pointLongitude'] as num).toDouble(),
        missionStatut: json['missionStatut'] as String?,
      );

  // EF-MAT-05/06 : "terminée" se dérive du statut réel de la Mission
  // (service-exe), jamais d'un état local — un ENLEVEMENT est fait dès que
  // la mission a dépassé EN_ATTENTE, une LIVRAISON seulement si LIVREE.
  bool get terminee => type == TypeEtapeTournee.enlevement
      ? const ['PRISE_EN_CHARGE', 'EN_TRANSIT', 'LIVREE'].contains(missionStatut)
      : missionStatut == 'LIVREE';
}

class TourneeMultiEtapes {
  final String tourneeId;
  final List<EtapeTournee> etapes;

  const TourneeMultiEtapes({required this.tourneeId, required this.etapes});

  int get indexEtapeCourante => etapes.indexWhere((e) => !e.terminee);

  bool get terminee => indexEtapeCourante == -1;

  EtapeTournee? get etapeCourante => terminee ? null : etapes[indexEtapeCourante];

  List<EtapeTournee> get etapesTerminees => etapes.where((e) => e.terminee).toList();
}

class MissionMultiEtapesState {
  final bool chargement;
  final String? erreur;
  final TourneeMultiEtapes? tournee;

  const MissionMultiEtapesState({this.chargement = false, this.erreur, this.tournee});

  MissionMultiEtapesState copyWith({bool? chargement, String? erreur, TourneeMultiEtapes? tournee}) =>
      MissionMultiEtapesState(
        chargement: chargement ?? this.chargement,
        erreur: erreur,
        tournee: tournee ?? this.tournee,
      );
}

// S11 (EF-MAT-05/06) : GET /missions/tournees/{tourneeId} — voir
// MissionExecutionController (gateway), qui consomme TourneeConstituee
// (service-opt) via service-exe. Remplace le MOCK initial — même contrat
// que MissionExecutionController (S7), généralisé à N étapes.
class MissionMultiEtapesNotifier extends StateNotifier<MissionMultiEtapesState> {
  final Dio _dio;

  MissionMultiEtapesNotifier(this._dio) : super(const MissionMultiEtapesState());

  Future<void> chargerTournee(String tourneeId) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/missions/tournees/$tourneeId');
      final etapes = (response.data['etapes'] as List<dynamic>)
          .map((e) => EtapeTournee.fromJson(e as Map<String, dynamic>))
          .toList();
      state = state.copyWith(chargement: false, tournee: TourneeMultiEtapes(tourneeId: tourneeId, etapes: etapes));
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
    }
  }

  // Une étape de tournée ENLEVEMENT/LIVRAISON se confirme par le même
  // endpoint que le flux S7 mono-étape (PRISE_EN_CHARGE/LIVRAISON), sur la
  // Mission précise à laquelle l'étape courante appartient — pas de nouvel
  // endpoint dédié côté backend.
  Future<bool> confirmerEtapeCourante() async {
    final tournee = state.tournee;
    final etape = tournee?.etapeCourante;
    if (tournee == null || etape == null) return false;

    final type = etape.type == TypeEtapeTournee.enlevement ? 'PRISE_EN_CHARGE' : 'LIVRAISON';
    final libelle = etape.type == TypeEtapeTournee.enlevement
        ? 'Enlèvement (tournée groupée)'
        : 'Livraison (tournée groupée)';

    state = state.copyWith(chargement: true, erreur: null);
    try {
      await _dio.post('/missions/${etape.missionId}/etapes', data: {'type': type, 'libelle': libelle});
      await chargerTournee(tournee.tourneeId);
      return true;
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
      return false;
    }
  }

  String _messageErreur(DioException e) {
    final status = e.response?.statusCode;
    if (status == 404) return 'Tournée introuvable.';
    if (status == 400) {
      return (e.response?.data is Map ? e.response?.data['detail'] as String? : null) ?? 'Requête refusée.';
    }
    if (status == 503) return 'Service d\'exécution momentanément indisponible.';
    return 'Erreur de connexion. Vérifiez votre réseau.';
  }
}

final missionMultiEtapesProvider =
    StateNotifierProvider<MissionMultiEtapesNotifier, MissionMultiEtapesState>((ref) {
  return MissionMultiEtapesNotifier(ref.watch(dioProvider));
});
