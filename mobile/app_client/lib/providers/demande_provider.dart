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
    // Tarification par chauffeur (spec-tarification-par-chauffeur.md) :
    // STANDARD | FRAGILE | ULTRA_FRAGILE -- sert au calcul de l'intervalle
    // de prix une fois le contrat correspondant branche cote backend
    // (TarificationL4Service, cote Moteur). Transmis des maintenant : un
    // champ JSON inconnu est sans effet pour un backend qui ne le lit pas
    // encore, pas besoin de revenir sur ce provider quand il sera pret.
    required String categorieMarchandise,
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
        'categorieMarchandise': categorieMarchandise,
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

  // CRUD demande (audit de suivi Mobile) : annulation plutôt que
  // suppression physique — StatutDemande.ANNULEE existe côté backend
  // depuis le début mais n'était jamais atteint par aucun code.
  Future<String?> annulerDemande(String demandeId) async {
    try {
      await _dio.delete('/demandes/$demandeId');
      await chargerMesDemandes();
      return null;
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      if (status == 409) {
        return (e.response?.data is String ? e.response?.data as String : null) == 'DEMANDE_DEJA_ACCEPTEE'
            ? 'Impossible d\'annuler — une proposition a déjà été acceptée.'
            : 'Cette demande est déjà annulée.';
      }
      return 'Erreur de connexion. Vérifiez votre réseau.';
    }
  }
}

final demandeProvider = StateNotifierProvider<DemandeNotifier, DemandeState>((ref) {
  return DemandeNotifier(ref.watch(mktDioProvider));
});
