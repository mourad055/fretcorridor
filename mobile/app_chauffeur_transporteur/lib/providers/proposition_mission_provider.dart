import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

class PropositionMission {
  final String affectationId;
  final String demandeId;
  final String capaciteId;
  final double? prixTransport;
  final String? origineNom;
  final String? destinationNom;
  final double? distanceMetres;
  final double? dureeSecondes;
  final String statut;
  final DateTime? expireA;
  final DateTime? dateCreation;

  const PropositionMission({
    required this.affectationId,
    required this.demandeId,
    required this.capaciteId,
    required this.statut,
    this.prixTransport,
    this.origineNom,
    this.destinationNom,
    this.distanceMetres,
    this.dureeSecondes,
    this.expireA,
    this.dateCreation,
  });

  factory PropositionMission.fromJson(Map<String, dynamic> json) => PropositionMission(
        affectationId: json['affectationId'] as String,
        demandeId: json['demandeId'] as String,
        capaciteId: json['capaciteId'] as String,
        statut: json['statut'] as String,
        prixTransport: (json['prixTransport'] as num?)?.toDouble(),
        origineNom: json['origineNom'] as String?,
        destinationNom: json['destinationNom'] as String?,
        distanceMetres: (json['distanceMetres'] as num?)?.toDouble(),
        dureeSecondes: (json['dureeSecondes'] as num?)?.toDouble(),
        expireA: json['expireA'] != null ? DateTime.tryParse(json['expireA'] as String) : null,
        dateCreation: json['dateCreation'] != null ? DateTime.tryParse(json['dateCreation'] as String) : null,
      );
}

class PropositionMissionState {
  final bool chargement;
  final String? erreur;
  final List<PropositionMission> propositions;

  const PropositionMissionState({this.chargement = false, this.erreur, this.propositions = const []});

  List<PropositionMission> get enAttente => propositions.where((p) => p.statut == 'PROPOSEE').toList();

  PropositionMissionState copyWith({bool? chargement, String? erreur, List<PropositionMission>? propositions}) {
    return PropositionMissionState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      propositions: propositions ?? this.propositions,
    );
  }
}

// UC-MAT-02/diffusion-course (30/08) : "Mes propositions" -- rebranche sur
// service-cap (Kafka demande-acceptee/demande-refusee-par-chauffeur +
// lecture synchrone GET /api/opt/affectations/proposees relayee par
// service-cap, voir plan-fretcorridor-reorientation.md §1/§9). Remplace le
// modele CDC strict (PropositionMission cote service-opt, PR #140 fermee) :
// une meme demande peut desormais etre diffusee a plusieurs chauffeurs en
// parallele, premier acceptant gagne.
class PropositionMissionNotifier extends StateNotifier<PropositionMissionState> {
  final Dio _dio;

  PropositionMissionNotifier(this._dio) : super(const PropositionMissionState());

  Future<void> charger() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/transporteur/propositions');
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
  /// 'reseau') que l'écran traduit -- ex. course perdue au profit d'un autre
  /// chauffeur entre-temps (diffusion-course, premier arrivé gagne).
  Future<String?> accepter(PropositionMission p) async {
    try {
      await _dio.post('/transporteur/propositions/${p.affectationId}/accepter',
          data: {'demandeId': p.demandeId, 'capaciteId': p.capaciteId});
      await charger();
      return null;
    } on DioException catch (e) {
      await charger();
      return _codeErreur(e);
    }
  }

  Future<String?> refuser(PropositionMission p) async {
    try {
      await _dio.post('/transporteur/propositions/${p.affectationId}/refuser',
          data: {'demandeId': p.demandeId, 'capaciteId': p.capaciteId});
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
