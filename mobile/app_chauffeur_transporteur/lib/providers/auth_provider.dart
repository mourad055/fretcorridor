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

  const AuthState({
    this.estConnecte = false,
    this.chargement = false,
    this.erreur,
    this.role,
    this.tenantId,
  });

  AuthState copyWith({
    bool? estConnecte,
    bool? chargement,
    String? erreur,
    String? role,
    String? tenantId,
  }) {
    return AuthState(
      estConnecte: estConnecte ?? this.estConnecte,
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      role: role ?? this.role,
      tenantId: tenantId ?? this.tenantId,
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

  AuthNotifier(this._dio) : super(const AuthState()) {
    _verifierSession();
  }

  Future<void> _verifierSession() async {
    final token = await _storage.read(key: keyAccessToken);
    if (token != null) {
      final role = await _storage.read(key: keyRole);
      final tenantId = await _storage.read(key: keyTenantId);
      state = state.copyWith(estConnecte: true, role: role, tenantId: tenantId);
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
      if (tenantId != null) {
        await _storage.write(key: keyTenantId, value: tenantId);
      }

      state = state.copyWith(
        chargement: false,
        estConnecte: true,
        role: role,
        tenantId: tenantId,
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
      state = state.copyWith(chargement: false, erreur: erreur);
      return false;
    }
  }

  Future<void> logout() async {
    await _storage.delete(key: keyAccessToken);
    await _storage.delete(key: keyRole);
    await _storage.delete(key: keyTenantId);
    state = const AuthState();
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(ref.watch(dioProvider));
});
