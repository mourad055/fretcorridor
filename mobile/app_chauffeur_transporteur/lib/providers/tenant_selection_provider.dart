import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

class TenantOption {
  final String id;
  final bool origine;
  const TenantOption({required this.id, required this.origine});

  factory TenantOption.fromJson(Map<String, dynamic> json) =>
      TenantOption(id: json['tenantId'] as String, origine: json['origine'] as bool);
}

class TenantSelectionState {
  final bool chargement;
  final String? erreur;
  final List<TenantOption> tenants;

  const TenantSelectionState({this.chargement = false, this.erreur, this.tenants = const []});

  TenantSelectionState copyWith({bool? chargement, String? erreur, List<TenantOption>? tenants}) {
    return TenantSelectionState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      tenants: tenants ?? this.tenants,
    );
  }
}

/// S18 (Sprint 18, "Second tenant institutionnel") — appel réel depuis le
/// 23 août : GET /api/v1/auth/tenants (gateway) renvoie le tenant d'origine
/// de l'acteur + toute affiliation accordée par un autre bureau (jamais
/// demandée par le transporteur lui-même — règle produit choisie : c'est le
/// second bureau qui invite/valide, l'invitation EST la validation).
class TenantSelectionNotifier extends StateNotifier<TenantSelectionState> {
  final Dio _dio;

  TenantSelectionNotifier(this._dio) : super(const TenantSelectionState());

  /// Retourne true si un choix de tenant est nécessaire (l'acteur est
  /// affilié à plusieurs tenants) — false sinon (cas normal, mono-tenant,
  /// aucun écran à afficher). false aussi en cas d'échec réseau : ne jamais
  /// bloquer la connexion sur cet appel best-effort (ENF-DIS-04).
  Future<bool> resoudrePourCompte() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/auth/tenants');
      final tenants = (response.data as List<dynamic>)
          .map((e) => TenantOption.fromJson(e as Map<String, dynamic>))
          .toList();
      state = state.copyWith(chargement: false, tenants: tenants);
      return tenants.length > 1;
    } on DioException catch (_) {
      state = state.copyWith(chargement: false, tenants: const []);
      return false;
    }
  }
}

final tenantSelectionProvider = StateNotifierProvider<TenantSelectionNotifier, TenantSelectionState>((ref) {
  return TenantSelectionNotifier(ref.watch(dioProvider));
});
