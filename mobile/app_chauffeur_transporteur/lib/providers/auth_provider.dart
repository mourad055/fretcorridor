import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'dio_provider.dart';

class AuthState {
  final bool estConnecte;
  final bool chargement;
  final String? erreur;
  final String? role;
  final String? tenantId;
  final String? telephone;

  const AuthState({
    this.estConnecte = false,
    this.chargement = false,
    this.erreur,
    this.role,
    this.tenantId,
    this.telephone,
  });

  AuthState copyWith({
    bool? estConnecte,
    bool? chargement,
    String? erreur,
    String? role,
    String? tenantId,
    String? telephone,
  }) {
    return AuthState(
      estConnecte: estConnecte ?? this.estConnecte,
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      role: role ?? this.role,
      tenantId: tenantId ?? this.tenantId,
      telephone: telephone ?? this.telephone,
    );
  }
}

// Contrat réel du gateway (POST /api/v1/auth/login) : {phone, code} en
// entrée, {token, role, tenantId} en sortie — un seul token, pas de refresh
// (aucun endpoint /auth/refresh n'existe côté gateway aujourd'hui), un seul
// rôle (String, pas une liste). Voir AuthController/LoginRequest/LoginResponse
// (backend/gateway/.../infrastructure/rest/).
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

  // Un message d'erreur affiché indéfiniment finit par sembler figé/bloqué —
  // il s'efface tout seul après quelques secondes plutôt que de rester tant
  // qu'aucune nouvelle action ne le remplace.
  void _afficherErreurTemporaire(String erreur) {
    _effacementErreur?.cancel();
    state = state.copyWith(chargement: false, erreur: erreur);
    _effacementErreur = Timer(const Duration(seconds: 5), () {
      if (mounted) state = state.copyWith(erreur: null);
    });
  }

  Future<void> _verifierSession() async {
    final token = await _storage.read(key: keyAccessToken);
    if (token != null) {
      final role = await _storage.read(key: keyRole);
      final tenantId = await _storage.read(key: keyTenantId);
      final telephone = await _storage.read(key: keyTelephone);
      state = state.copyWith(estConnecte: true, role: role, tenantId: tenantId, telephone: telephone);
    }
  }

  Future<bool> login(String telephone, String code) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.post('/auth/login', data: {
        'phone': telephone,
        'code': code,
      });
      final token = response.data['token'] as String;
      final role = response.data['role'] as String;
      final tenantId = response.data['tenantId'] as String?;

      await _storage.write(key: keyAccessToken, value: token);
      await _storage.write(key: keyRole, value: role);
      await _storage.write(key: keyTelephone, value: telephone);
      if (tenantId != null) {
        await _storage.write(key: keyTenantId, value: tenantId);
      }

      state = state.copyWith(
        chargement: false,
        estConnecte: true,
        role: role,
        tenantId: tenantId,
        telephone: telephone,
      );
      return true;
    } on DioException catch (e) {
      String erreur;
      final status = e.response?.statusCode;
      if (status == 401) {
        erreur = 'Numéro de téléphone ou code invalide.';
      } else if (status == 503) {
        erreur = 'Service d\'authentification momentanément indisponible.';
      } else {
        erreur = 'Erreur de connexion. Vérifiez votre réseau.';
      }
      _afficherErreurTemporaire(erreur);
      return false;
    }
  }

  Future<bool> register({
    required String telephone,
    required String code,
    required String type,
    String? nom,
    String? prenom,
    String? raisonSociale,
  }) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.post('/auth/register', data: {
        'phone': telephone,
        'code': code,
        'type': type,
        if (nom != null) 'nom': nom,
        if (prenom != null) 'prenom': prenom,
        if (raisonSociale != null) 'raisonSociale': raisonSociale,
      });
      final token = response.data['token'] as String;
      final role = response.data['role'] as String;
      final tenantId = response.data['tenantId'] as String?;

      await _storage.write(key: keyAccessToken, value: token);
      await _storage.write(key: keyRole, value: role);
      await _storage.write(key: keyTelephone, value: telephone);
      if (tenantId != null) {
        await _storage.write(key: keyTenantId, value: tenantId);
      }

      state = state.copyWith(
        chargement: false,
        estConnecte: true,
        role: role,
        tenantId: tenantId,
        telephone: telephone,
      );
      return true;
    } on DioException catch (e) {
      String erreur;
      final status = e.response?.statusCode;
      if (status == 400) {
        erreur = (e.response?.data is Map ? e.response?.data['detail'] as String? : null) ?? 'Inscription refusée.';
      } else if (status == 503) {
        erreur = 'Service d\'authentification momentanément indisponible.';
      } else {
        erreur = 'Erreur de connexion. Vérifiez votre réseau.';
      }
      _afficherErreurTemporaire(erreur);
      return false;
    }
  }

  Future<void> logout() async {
    await _storage.delete(key: keyAccessToken);
    await _storage.delete(key: keyRole);
    await _storage.delete(key: keyTenantId);
    await _storage.delete(key: keyTelephone);
    state = const AuthState();
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(ref.watch(dioProvider));
});
