import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

enum StatutProposition { enAttente, acceptee, refusee }

class PropositionRetour {
  final String id;
  final String titre;
  final String corps;
  final StatutProposition statut;

  const PropositionRetour({
    required this.id,
    required this.titre,
    required this.corps,
    required this.statut,
  });

  factory PropositionRetour.fromJson(Map<String, dynamic> json) {
    final reponse = json['reponseAcceptee'] as bool?;
    final statut = reponse == null
        ? StatutProposition.enAttente
        : (reponse ? StatutProposition.acceptee : StatutProposition.refusee);
    return PropositionRetour(
      id: json['id'] as String,
      titre: json['titre'] as String,
      corps: json['corps'] as String,
      statut: statut,
    );
  }

  PropositionRetour copyWith({StatutProposition? statut}) => PropositionRetour(
        id: id,
        titre: titre,
        corps: corps,
        statut: statut ?? this.statut,
      );
}

class PropositionRetourState {
  final List<PropositionRetour> propositions;

  const PropositionRetourState({this.propositions = const []});

  List<PropositionRetour> get enAttente =>
      propositions.where((p) => p.statut == StatutProposition.enAttente).toList();
}

/// S12 (Sprint 12, "Retour à vide") — branché sur le vrai backend depuis le
/// 18 août. service-not consomme désormais réellement
/// `proposition-retour-a-vide` (service-opt, Moteur) et crée une
/// notification de type `PROPOSITION_RETOUR` (voir
/// service-not/messaging/PropositionRetourAVideListener.java). Ce provider
/// filtre ce type parmi `/notifications/mes` et répond via
/// `PATCH /notifications/mes/{id}/repondre` — voir NotificationMobileController
/// (gateway). La réponse reste locale à service-not : aucun contrat n'existe
/// à ce jour pour la relayer au Moteur.
class PropositionRetourNotifier extends StateNotifier<PropositionRetourState> {
  final Dio _dio;

  PropositionRetourNotifier(this._dio) : super(const PropositionRetourState());

  Future<void> charger() async {
    try {
      final response = await _dio.get('/notifications/mes');
      final propositions = (response.data as List<dynamic>)
          .cast<Map<String, dynamic>>()
          .where((n) => n['type'] == 'PROPOSITION_RETOUR')
          .map(PropositionRetour.fromJson)
          .toList();
      state = PropositionRetourState(propositions: propositions);
    } on DioException {
      // Pas bloquant — l'écran affiche déjà une erreur pour la liste
      // principale des notifications si le service est indisponible.
    }
  }

  Future<void> accepter(String id) => _repondre(id, true);

  Future<void> refuser(String id) => _repondre(id, false);

  Future<void> _repondre(String id, bool accepte) async {
    final avant = state.propositions;
    state = PropositionRetourState(
      propositions: [
        for (final p in avant)
          if (p.id == id) p.copyWith(statut: accepte ? StatutProposition.acceptee : StatutProposition.refusee)
          else p,
      ],
    );
    try {
      await _dio.patch('/notifications/mes/$id/repondre', data: {'accepte': accepte});
    } on DioException {
      state = PropositionRetourState(propositions: avant);
    }
  }
}

final propositionRetourProvider = StateNotifierProvider<PropositionRetourNotifier, PropositionRetourState>((ref) {
  return PropositionRetourNotifier(ref.watch(dioProvider));
});
