import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

class PropositionMission {
  final String id;
  final String demandeId;
  final double? prixTransport;
  final String? origineNom;
  final String? destinationNom;
  final String? typeEmballageNom;
  final int? quantite;
  final double? poidsTotalKg;
  final String? destinataireNom;
  final String? destinataireTelephone;
  final String? modeCollecte;
  final String? typeDisponibilite;
  final double? distanceMetres;
  final double? dureeSecondes;
  final bool? grandeValeur;
  final String statut;
  final DateTime? expireA;
  final DateTime? dateCreation;

  const PropositionMission({
    required this.id,
    required this.demandeId,
    required this.statut,
    this.prixTransport,
    this.origineNom,
    this.destinationNom,
    this.typeEmballageNom,
    this.quantite,
    this.poidsTotalKg,
    this.destinataireNom,
    this.destinataireTelephone,
    this.modeCollecte,
    this.typeDisponibilite,
    this.distanceMetres,
    this.dureeSecondes,
    this.grandeValeur,
    this.expireA,
    this.dateCreation,
  });

  factory PropositionMission.fromJson(Map<String, dynamic> json) => PropositionMission(
        id: json['id'] as String,
        demandeId: json['demandeId'] as String,
        statut: json['statut'] as String,
        prixTransport: (json['prixTransport'] as num?)?.toDouble(),
        origineNom: json['origineNom'] as String?,
        destinationNom: json['destinationNom'] as String?,
        typeEmballageNom: json['typeEmballageNom'] as String?,
        quantite: json['quantite'] as int?,
        poidsTotalKg: (json['poidsTotalKg'] as num?)?.toDouble(),
        destinataireNom: json['destinataireNom'] as String?,
        destinataireTelephone: json['destinataireTelephone'] as String?,
        modeCollecte: json['modeCollecte'] as String?,
        typeDisponibilite: json['typeDisponibilite'] as String?,
        distanceMetres: (json['distanceMetres'] as num?)?.toDouble(),
        dureeSecondes: (json['dureeSecondes'] as num?)?.toDouble(),
        grandeValeur: json['grandeValeur'] as bool?,
        expireA: json['expireA'] != null ? DateTime.tryParse(json['expireA'] as String) : null,
        dateCreation: json['dateCreation'] != null ? DateTime.tryParse(json['dateCreation'] as String) : null,
      );
}

class PropositionMissionState {
  final bool chargement;
  final String? erreur;
  final List<PropositionMission> propositions;

  const PropositionMissionState({this.chargement = false, this.erreur, this.propositions = const []});

  List<PropositionMission> get enAttente => propositions.where((p) => p.statut == 'EN_ATTENTE').toList();

  PropositionMissionState copyWith({bool? chargement, String? erreur, List<PropositionMission>? propositions}) {
    return PropositionMissionState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      propositions: propositions ?? this.propositions,
    );
  }
}

// UC-MAT-02 (CDC page 43) : "Mes propositions" -- acceptation/refus d'une
// mission par le chauffeur, relayé par la gateway vers service-opt (voir
// PropositionMissionController côté gateway -- entorse ADR 0013 assumée et
// documentée là-bas, à régulariser avant tout merge vers dev).
class PropositionMissionNotifier extends StateNotifier<PropositionMissionState> {
  final Dio _dio;

  PropositionMissionNotifier(this._dio) : super(const PropositionMissionState());

  Future<void> charger() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/propositions-mission/mes');
      state = state.copyWith(
        chargement: false,
        propositions: (response.data as List<dynamic>)
            .map((p) => PropositionMission.fromJson(p as Map<String, dynamic>))
            .toList(),
      );
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      state = state.copyWith(
        chargement: false,
        erreur: status == 503 ? 'Service momentanément indisponible.' : 'Erreur de connexion. Vérifiez votre réseau.',
      );
    }
  }

  /// Retourne null en cas de succès, ou un code d'erreur ('indisponible' |
  /// 'reseau') que l'écran traduit -- ex. E3 : un autre transporteur a déjà
  /// accepté cette proposition entre-temps.
  Future<String?> accepter(String id) async {
    try {
      await _dio.post('/propositions-mission/$id/accepter');
      await charger();
      return null;
    } on DioException catch (e) {
      await charger();
      return _codeErreur(e);
    }
  }

  Future<String?> refuser(String id, String motif) async {
    try {
      await _dio.post('/propositions-mission/$id/refuser', data: {'motif': motif});
      await charger();
      return null;
    } on DioException catch (e) {
      await charger();
      return _codeErreur(e);
    }
  }

  String _codeErreur(DioException e) {
    final status = e.response?.statusCode;
    if (status == 409 || status == 410 || status == 404) return 'indisponible';
    return 'reseau';
  }
}

final propositionMissionProvider = StateNotifierProvider<PropositionMissionNotifier, PropositionMissionState>((ref) {
  return PropositionMissionNotifier(ref.watch(dioProvider));
});
