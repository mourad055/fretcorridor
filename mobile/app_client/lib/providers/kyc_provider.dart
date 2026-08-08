import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'dio_provider.dart';

class KycState {
  final bool chargement;
  final String? erreur;
  final String? succes;
  final String? type; // PARTICULIER ou ENTREPRISE
  final String? nom;
  final String? prenom;
  final String? raisonSociale;
  final String niveauKyc;

  const KycState({
    this.chargement = false,
    this.erreur,
    this.succes,
    this.type,
    this.nom,
    this.prenom,
    this.raisonSociale,
    this.niveauKyc = 'NIVEAU_0',
  });

  KycState copyWith({
    bool? chargement,
    String? erreur,
    String? succes,
    String? type,
    String? nom,
    String? prenom,
    String? raisonSociale,
    String? niveauKyc,
  }) {
    return KycState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      succes: succes,
      type: type ?? this.type,
      nom: nom ?? this.nom,
      prenom: prenom ?? this.prenom,
      raisonSociale: raisonSociale ?? this.raisonSociale,
      niveauKyc: niveauKyc ?? this.niveauKyc,
    );
  }
}

class KycNotifier extends StateNotifier<KycState> {
  final Dio _dio;
  static const _storage = FlutterSecureStorage();

  KycNotifier(this._dio) : super(const KycState()) {
    chargerProfil();
  }

  Future<void> chargerProfil() async {
    try {
      final response = await _dio.get('/kyc/profil');
      state = state.copyWith(
        type: response.data['type'],
        nom: response.data['nom'],
        prenom: response.data['prenom'],
        raisonSociale: response.data['raisonSociale'],
        niveauKyc: response.data['niveauKyc'],
      );
    } on DioException {
      // Pas encore de profil complété — normal juste après l'inscription
    }
  }

  Future<bool> completerParticulier({required String nom, required String prenom}) async {
    state = state.copyWith(chargement: true, erreur: null, succes: null);
    try {
      final response = await _dio.put('/kyc/profil/particulier', data: {
        'nom': nom,
        'prenom': prenom,
      });
      await _enregistrerNouveauxTokens(response.data);
      state = state.copyWith(
        chargement: false,
        succes: 'Profil complété ✅',
        type: 'PARTICULIER',
        nom: response.data['profil']['nom'],
        prenom: response.data['profil']['prenom'],
        niveauKyc: response.data['profil']['niveauKyc'],
      );
      return true;
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: 'Erreur : ${e.response?.data ?? e.message}');
      return false;
    }
  }

  Future<bool> completerEntreprise({required String raisonSociale, String? numeroRegistreCommerce}) async {
    state = state.copyWith(chargement: true, erreur: null, succes: null);
    try {
      final response = await _dio.put('/kyc/profil/entreprise', data: {
        'raisonSociale': raisonSociale,
        if (numeroRegistreCommerce != null) 'numeroRegistreCommerce': numeroRegistreCommerce,
      });
      await _enregistrerNouveauxTokens(response.data);
      state = state.copyWith(
        chargement: false,
        succes: 'Profil complété ✅',
        type: 'ENTREPRISE',
        raisonSociale: response.data['profil']['raisonSociale'],
        niveauKyc: response.data['profil']['niveauKyc'],
      );
      return true;
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: 'Erreur : ${e.response?.data ?? e.message}');
      return false;
    }
  }

  // Après complétion du profil, le niveauKyc change → nouveaux tokens
  // nécessaires pour que les futurs appels (ex: publier une demande) portent
  // le bon niveau à jour.
  Future<void> _enregistrerNouveauxTokens(Map<String, dynamic> data) async {
    await _storage.write(key: keyAccessToken, value: data['accessToken']);
    await _storage.write(key: keyRefreshToken, value: data['refreshToken']);
  }
}

final kycProvider = StateNotifierProvider<KycNotifier, KycState>((ref) {
  return KycNotifier(ref.watch(dioProvider));
});
