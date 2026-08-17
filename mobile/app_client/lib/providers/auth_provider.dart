import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'dio_provider.dart';

class AuthState {
  final bool estConnecte;
  final bool chargement;
  final String? erreur;
  final List<String> roles;
  final String? tenantId;

  const AuthState({
    this.estConnecte = false,
    this.chargement = false,
    this.erreur,
    this.roles = const [],
    this.tenantId,
  });

  AuthState copyWith({
    bool? estConnecte,
    bool? chargement,
    String? erreur,
    List<String>? roles,
    String? tenantId,
  }) {
    return AuthState(
      estConnecte: estConnecte ?? this.estConnecte,
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      roles: roles ?? this.roles,
      tenantId: tenantId ?? this.tenantId,
    );
  }
}

class AuthNotifier extends StateNotifier<AuthState> {
  final Dio _dio;
  static const _storage = FlutterSecureStorage();

  AuthNotifier(this._dio) : super(const AuthState()) {
    _verifierSession();
  }

  Future<void> _verifierSession() async {
    final token = await _storage.read(key: keyAccessToken);
    if (token != null) {
      // Pas de KYC/profil à recharger au S1 — la session est juste "présente"
      state = state.copyWith(estConnecte: true);
    }
  }

  Future<bool> login(String telephone, String codePin) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.post('/auth/login', data: {
        'telephone': telephone,
        'codePin': codePin,
      });
      await _enregistrerSession(response.data);
      state = state.copyWith(
        chargement: false,
        estConnecte: true,
        roles: List<String>.from(response.data['roles']),
        tenantId: response.data['tenantId'],
      );
      return true;
    } on DioException catch (e) {
      final message = e.response?.data?.toString() ?? e.message ?? '';
      String erreur = 'Erreur de connexion. Vérifiez votre réseau.';
      if (message.contains('PIN_INCORRECT')) {
        erreur = 'Code PIN incorrect.';
      } else if (message.contains('COMPTE_BLOQUE')) {
        erreur = 'Compte bloqué après plusieurs tentatives.';
      } else if (message.contains('ACTEUR_INTROUVABLE')) {
        erreur = 'Numéro non reconnu.';
      }
      state = state.copyWith(chargement: false, erreur: erreur);
      return false;
    }
  }

  Future<bool> inscrireChargeur({
    required String telephone,
    required String codePin,
    String? nom,
    String? prenom,
    String? raisonSociale,
  }) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.post('/auth/inscription-chargeur', data: {
        'telephone': telephone,
        'codePin': codePin,
        if (nom != null) 'nom': nom,
        if (prenom != null) 'prenom': prenom,
        if (raisonSociale != null) 'raisonSociale': raisonSociale,
      });
      await _enregistrerSession(response.data);
      state = state.copyWith(
        chargement: false,
        estConnecte: true,
        roles: List<String>.from(response.data['roles']),
        tenantId: response.data['tenantId'],
      );
      return true;
    } on DioException catch (e) {
      final message = e.response?.data?.toString() ?? e.message ?? '';
      String erreur = 'Erreur lors de l\'inscription.';
      if (message.contains('TELEPHONE_DEJA_UTILISE')) {
        erreur = 'Ce numéro a déjà un compte — connectez-vous plutôt.';
      }
      state = state.copyWith(chargement: false, erreur: erreur);
      return false;
    }
  }

  Future<void> _enregistrerSession(Map<String, dynamic> data) async {
    await _storage.write(key: keyAccessToken, value: data['accessToken']);
    await _storage.write(key: keyRefreshToken, value: data['refreshToken']);
  }

  Future<void> logout() async {
    await _storage.delete(key: keyAccessToken);
    await _storage.delete(key: keyRefreshToken);
    state = const AuthState();
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(ref.watch(idaDioProvider));
});
