import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import '../models/catalogue_emballage_model.dart';
import '../models/demande_model.dart';
import 'dio_provider.dart';

class DemandeState {
  final List<CatalogueEmballageModel> catalogue;
  final List<DemandeModel> mesDemandes;
  final bool chargement;
  final bool publicationEnCours;
  final String? erreur;
  final String? succes;

  const DemandeState({
    this.catalogue = const [],
    this.mesDemandes = const [],
    this.chargement = false,
    this.publicationEnCours = false,
    this.erreur,
    this.succes,
  });

  DemandeState copyWith({
    List<CatalogueEmballageModel>? catalogue,
    List<DemandeModel>? mesDemandes,
    bool? chargement,
    bool? publicationEnCours,
    String? erreur,
    String? succes,
  }) {
    return DemandeState(
      catalogue: catalogue ?? this.catalogue,
      mesDemandes: mesDemandes ?? this.mesDemandes,
      chargement: chargement ?? this.chargement,
      publicationEnCours: publicationEnCours ?? this.publicationEnCours,
      erreur: erreur,
      succes: succes,
    );
  }
}

class DemandeNotifier extends StateNotifier<DemandeState> {
  final Dio _dio;

  DemandeNotifier(this._dio) : super(const DemandeState()) {
    chargerCatalogue();
    chargerMesDemandes();
  }

  Future<void> chargerCatalogue() async {
    try {
      final response = await _dio.get('/catalogue-emballages');
      state = state.copyWith(
        catalogue: (response.data as List).map((e) => CatalogueEmballageModel.fromJson(e)).toList(),
      );
    } on DioException catch (e) {
      state = state.copyWith(erreur: 'Impossible de charger le catalogue : ${e.message}');
    }
  }

  Future<void> chargerMesDemandes() async {
    state = state.copyWith(chargement: true);
    try {
      final response = await _dio.get('/demandes/mes-demandes');
      state = state.copyWith(
        chargement: false,
        mesDemandes: (response.data as List).map((e) => DemandeModel.fromJson(e)).toList(),
      );
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: 'Impossible de charger vos demandes : ${e.message}');
    }
  }

  Future<bool> publier({
    required String villeDepart,
    required String villeArrivee,
    required String typeEmballageId,
    required int quantite,
    bool fragile = false,
    bool perissable = false,
    bool dangereuse = false,
    bool grandeValeur = false,
    required String typeDisponibilite,
    DateTime? dateDisponibilite,
    required String modeCollecte,
    required String destinataireNom,
    required String destinataireTelephone,
  }) async {
    state = state.copyWith(publicationEnCours: true, erreur: null, succes: null);
    try {
      await _dio.post('/demandes', data: {
        'villeDepart': villeDepart,
        'villeArrivee': villeArrivee,
        'typeEmballageId': typeEmballageId,
        'quantite': quantite,
        'fragile': fragile,
        'perissable': perissable,
        'dangereuse': dangereuse,
        'grandeValeur': grandeValeur,
        'typeDisponibilite': typeDisponibilite,
        if (dateDisponibilite != null) 'dateDisponibilite': dateDisponibilite.toIso8601String(),
        'modeCollecte': modeCollecte,
        'destinataireNom': destinataireNom,
        'destinataireTelephone': destinataireTelephone,
      });
      state = state.copyWith(publicationEnCours: false, succes: 'Demande publiée ✅');
      await chargerMesDemandes();
      return true;
    } on DioException catch (e) {
      final message = e.response?.data?.toString() ?? e.message ?? '';
      String erreur = 'Erreur lors de la publication.';
      if (message.contains('KYC_INSUFFISANT')) {
        erreur = 'Complétez votre profil avant de publier une demande.';
      }
      state = state.copyWith(publicationEnCours: false, erreur: erreur);
      return false;
    }
  }

  // S5 — peut renvoyer une liste vide tant que le moteur de matching
  // (service-mat/service-opt, côté Moteur) n'est pas branché.
  Future<List<Map<String, dynamic>>> getPropositions(String demandeId) async {
    try {
      final response = await _dio.get('/demandes/$demandeId/propositions');
      return List<Map<String, dynamic>>.from(response.data);
    } on DioException {
      return [];
    }
  }
}

final demandeProvider = StateNotifierProvider<DemandeNotifier, DemandeState>((ref) {
  return DemandeNotifier(ref.watch(mktDioProvider));
});
