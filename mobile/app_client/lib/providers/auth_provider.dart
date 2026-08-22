import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'dio_provider.dart';

const String keyTelephone = 'telephone';
const String keyTenantId = 'tenant_id';

class AuthState {
  final bool estConnecte;
  final bool chargement;
  final String? erreur;
  final List<String> roles;
  final String? tenantId;
  final String? telephone;

  const AuthState({
    this.estConnecte = false,
    this.chargement = false,
    this.erreur,
    this.roles = const [],
    this.tenantId,
    this.telephone,
  });

  AuthState copyWith({
    bool? estConnecte,
    bool? chargement,
    String? erreur,
    List<String>? roles,
    String? tenantId,
    String? telephone,
  }) {
    return AuthState(
      estConnecte: estConnecte ?? this.estConnecte,
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      roles: roles ?? this.roles,
      tenantId: tenantId ?? this.tenantId,
      telephone: telephone ?? this.telephone,
    );
  }
}

class AuthNotifier extends StateNotifier<AuthState> {
  final Dio _dio;
  static const _storage = FlutterSecureStorage();
  Timer? _effacementErreur;

  AuthNotifier(this._dio) : super(const AuthState()) {
    _verifierSession();
  }

  @override
  void dispose() {
    _effacementErreur?.cancel();
    super.dispose();
  }

  // Un message d'erreur affiché indéfiniment finit par sembler figé — il
  // s'efface tout seul après quelques secondes plutôt que de rester tant
  // qu'aucune nouvelle action ne le remplace.
  void _afficherErreurTemporaire(String erreur) {
    _effacementErreur?.cancel();
    state = state.copyWith(chargement: false, erreur: erreur);
    _effacementErreur = Timer(const Duration(seconds: 5), () {
      if (mounted) state = state.copyWith(erreur: null);
    });
  }

  // BUG CORRIGE (retour utilisateur direct, 22 aout) : tenantId n'etait
  // jamais persiste ni restaure ici (seulement telephone) - toute session
  // restauree depuis le stockage (app relancee sans nouvelle connexion,
  // ce qui arrive tres souvent en test) avait estConnecte=true mais
  // tenantId=null, faisant echouer silencieusement tout appel qui en
  // depend (ex. confirmation du moyen de paiement -> "Session incomplete").
  Future<void> _verifierSession() async {
    final token = await _storage.read(key: keyAccessToken);
    if (token != null) {
      final telephone = await _storage.read(key: keyTelephone);
      final tenantId = await _storage.read(key: keyTenantId);
      state = state.copyWith(estConnecte: true, telephone: telephone, tenantId: tenantId);
    }
  }

  Future<bool> login(String telephone, String codePin) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.post('/auth/login', data: {
        'telephone': telephone,
        'codePin': codePin,
      });
      await _enregistrerSession(response.data, telephone);
      state = state.copyWith(
        chargement: false,
        estConnecte: true,
        roles: List<String>.from(response.data['roles']),
        tenantId: response.data['tenantId'],
        telephone: telephone,
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
      _afficherErreurTemporaire(erreur);
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
      await _enregistrerSession(response.data, telephone);
      state = state.copyWith(
        chargement: false,
        estConnecte: true,
        roles: List<String>.from(response.data['roles']),
        tenantId: response.data['tenantId'],
        telephone: telephone,
      );
      return true;
    } on DioException catch (e) {
      final message = e.response?.data?.toString() ?? e.message ?? '';
      String erreur = 'Erreur lors de l\'inscription.';
      if (message.contains('TELEPHONE_DEJA_UTILISE')) {
        erreur = 'Ce numéro a déjà un compte — connectez-vous plutôt.';
      }
      _afficherErreurTemporaire(erreur);
      return false;
    }
  }

  // Vérification de l'ancien numéro côté serveur (AuthController.modifierTelephone,
  // service-ida) avant tout changement — même principe de sécurité que le PIN au login.
  Future<bool> modifierTelephone(String ancienTelephone, String nouveauTelephone) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final token = await _storage.read(key: keyAccessToken);
      final response = await _dio.put('/auth/telephone',
          data: {'ancienTelephone': ancienTelephone, 'nouveauTelephone': nouveauTelephone},
          options: Options(headers: {'Authorization': 'Bearer $token'}));
      final nouveauNumero = response.data['telephone'] as String;
      await _storage.write(key: keyTelephone, value: nouveauNumero);
      state = state.copyWith(chargement: false, telephone: nouveauNumero);
      return true;
    } on DioException catch (e) {
      final message = e.response?.data?.toString() ?? e.message ?? '';
      String erreur = 'Erreur lors du changement de numéro.';
      if (message.contains('ANCIEN_TELEPHONE_INCORRECT')) {
        erreur = 'L\'ancien numéro saisi est incorrect.';
      } else if (message.contains('TELEPHONE_DEJA_UTILISE')) {
        erreur = 'Ce numéro est déjà utilisé par un autre compte.';
      }
      _afficherErreurTemporaire(erreur);
      return false;
    }
  }

  Future<void> _enregistrerSession(Map<String, dynamic> data, String telephone) async {
    await _storage.write(key: keyAccessToken, value: data['accessToken']);
    await _storage.write(key: keyRefreshToken, value: data['refreshToken']);
    await _storage.write(key: keyTelephone, value: telephone);
    if (data['tenantId'] != null) {
      await _storage.write(key: keyTenantId, value: data['tenantId'] as String);
    }
  }

  Future<void> logout() async {
    await _storage.delete(key: keyAccessToken);
    await _storage.delete(key: keyRefreshToken);
    await _storage.delete(key: keyTelephone);
    await _storage.delete(key: keyTenantId);
    state = const AuthState();
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(ref.watch(idaDioProvider));
});
