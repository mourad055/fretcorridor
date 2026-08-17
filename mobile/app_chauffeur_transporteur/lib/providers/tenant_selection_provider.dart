import 'package:flutter_riverpod/legacy.dart';

class TenantOption {
  final String id;
  final String nom;
  const TenantOption({required this.id, required this.nom});
}

class TenantSelectionState {
  final List<TenantOption> tenants;
  final String? tenantSelectionneId;

  const TenantSelectionState({this.tenants = const [], this.tenantSelectionneId});
}

/// S18 (Sprint 18, "Second tenant institutionnel") — ⚠️ MOCK, aucun appel
/// réseau. service-ida + gateway ne renvoient aujourd'hui qu'un seul
/// tenantId par compte (LoginResponse, cf auth_provider.dart) — aucun
/// contrat multi-tenant par acteur à ce jour côté serveur. Pour démontrer
/// le sélecteur sans backend, un seul numéro de test fictif
/// (_telephoneDemoMultiTenant, arbitraire, à but de démonstration
/// uniquement) déclenche 2 tenants simulés ; tout autre compte reste
/// mono-tenant (comportement inchangé, écran sauté automatiquement) — à
/// remplacer par un vrai GET des tenants du compte une fois
/// service-ida/gateway prêts côté Web.
class TenantSelectionNotifier extends StateNotifier<TenantSelectionState> {
  TenantSelectionNotifier() : super(const TenantSelectionState());

  static const _telephoneDemoMultiTenant = '+237690000099';

  /// Retourne true si un choix de tenant est nécessaire (plusieurs tenants
  /// mockés pour ce compte) — false sinon (cas normal, mono-tenant réel,
  /// aucun écran à afficher).
  bool resoudrePourCompte(String telephone, String tenantIdReel) {
    if (telephone.trim() != _telephoneDemoMultiTenant) {
      state = TenantSelectionState(
        tenants: [TenantOption(id: tenantIdReel, nom: 'Mon bureau')],
        tenantSelectionneId: tenantIdReel,
      );
      return false;
    }

    state = TenantSelectionState(
      tenants: [
        TenantOption(id: tenantIdReel, nom: 'Bureau Douala (principal)'),
        const TenantOption(id: 'mock-tenant-s18-002', nom: 'Bureau Yaoundé (démo)'),
      ],
    );
    return true;
  }

  void selectionner(String tenantId) {
    state = TenantSelectionState(tenants: state.tenants, tenantSelectionneId: tenantId);
  }
}

final tenantSelectionProvider = StateNotifierProvider<TenantSelectionNotifier, TenantSelectionState>((ref) {
  return TenantSelectionNotifier();
});
