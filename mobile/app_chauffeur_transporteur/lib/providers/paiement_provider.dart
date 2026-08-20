import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

class Ecriture {
  final String id;
  final String missionId;
  final String nature;
  final String sens;
  final double montant;
  final String creeLe;
  final String statut;

  const Ecriture({
    required this.id,
    required this.missionId,
    required this.nature,
    required this.sens,
    required this.montant,
    required this.creeLe,
    required this.statut,
  });

  factory Ecriture.fromJson(Map<String, dynamic> json) => Ecriture(
        id: json['id'] as String,
        missionId: json['missionId'] as String,
        nature: json['nature'] as String,
        sens: json['sens'] as String,
        montant: (json['montant'] as num).toDouble(),
        creeLe: json['creeLe'] as String,
        statut: json['statut'] as String,
      );
}

class PaiementState {
  final bool chargement;
  final String? erreur;
  final double solde;
  final List<Ecriture> historique;
  final Map<String, String> modePaiementParMission;

  const PaiementState({
    this.chargement = false,
    this.erreur,
    this.solde = 0,
    this.historique = const [],
    this.modePaiementParMission = const {},
  });

  PaiementState copyWith({
    bool? chargement,
    String? erreur,
    double? solde,
    List<Ecriture>? historique,
    Map<String, String>? modePaiementParMission,
  }) {
    return PaiementState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      solde: solde ?? this.solde,
      historique: historique ?? this.historique,
      modePaiementParMission: modePaiementParMission ?? this.modePaiementParMission,
    );
  }
}

// S8 (EF-PAY, lecture seule — ENF-FIN-01) : GET /paiement — voir
// PaiementReadController.monSolde (backend/gateway/.../infrastructure/rest/pay/).
// Consomme service-pay réel (grand livre miroir), déjà construit par l'équipe.
class PaiementNotifier extends StateNotifier<PaiementState> {
  final Dio _dio;

  PaiementNotifier(this._dio) : super(const PaiementState());

  Future<void> chargerSolde() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/paiement');
      final historique = (response.data['historique'] as List<dynamic>)
          .map((e) => Ecriture.fromJson(e as Map<String, dynamic>))
          .toList();
      state = state.copyWith(
        chargement: false,
        solde: (response.data['solde'] as num).toDouble(),
        historique: historique,
      );
      await _chargerModesPaiement(historique);
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
    }
  }

  // S14 (EF-PAY-06/07) : moyen de paiement choisi par le client, pertinent
  // uniquement pour les écritures d'ENCAISSEMENT. Un échec/404 par mission
  // (rien choisi encore) reste silencieux — l'écran n'affiche simplement
  // rien pour cette écriture, comme le mock qu'il remplace.
  Future<void> _chargerModesPaiement(List<Ecriture> historique) async {
    final missionIds = historique.where((e) => e.nature == 'ENCAISSEMENT').map((e) => e.missionId).toSet();
    final modes = <String, String>{};
    for (final missionId in missionIds) {
      try {
        final response = await _dio.get('/paiement/missions/$missionId/moyen-paiement');
        modes[missionId] = response.data['modePaiement'] as String;
      } on DioException {
        // Pas encore choisi (404) ou service indisponible — pas bloquant.
      }
    }
    state = state.copyWith(modePaiementParMission: modes);
  }

  String _messageErreur(DioException e) {
    if (e.response?.statusCode == 503) return 'Service de paiement momentanément indisponible.';
    return 'Erreur de connexion. Vérifiez votre réseau.';
  }
}

final paiementProvider = StateNotifierProvider<PaiementNotifier, PaiementState>((ref) {
  return PaiementNotifier(ref.watch(dioProvider));
});
