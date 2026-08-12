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

  const PaiementState({this.chargement = false, this.erreur, this.solde = 0, this.historique = const []});

  PaiementState copyWith({bool? chargement, String? erreur, double? solde, List<Ecriture>? historique}) {
    return PaiementState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      solde: solde ?? this.solde,
      historique: historique ?? this.historique,
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
      state = state.copyWith(
        chargement: false,
        solde: (response.data['solde'] as num).toDouble(),
        historique: (response.data['historique'] as List<dynamic>)
            .map((e) => Ecriture.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
    }
  }

  String _messageErreur(DioException e) {
    if (e.response?.statusCode == 503) return 'Service de paiement momentanément indisponible.';
    return 'Erreur de connexion. Vérifiez votre réseau.';
  }
}

final paiementProvider = StateNotifierProvider<PaiementNotifier, PaiementState>((ref) {
  return PaiementNotifier(ref.watch(dioProvider));
});
