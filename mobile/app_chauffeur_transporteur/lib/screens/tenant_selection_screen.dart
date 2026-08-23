import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/auth_provider.dart';
import '../providers/tenant_selection_provider.dart';
import '../theme/app_theme.dart';
import 'kyc_screen.dart';

/// S18 (Sprint 18, "Second tenant institutionnel") — appel réel depuis le
/// 23 août, voir tenant_selection_provider.dart. Affiché uniquement après
/// authentification réussie, avant l'accès à l'app, et seulement si le
/// compte est rattaché à plusieurs tenants — le cas normal (mono-tenant) ne
/// passe jamais par cet écran.
class TenantSelectionScreen extends ConsumerStatefulWidget {
  const TenantSelectionScreen({super.key});

  @override
  ConsumerState<TenantSelectionScreen> createState() => _TenantSelectionScreenState();
}

class _TenantSelectionScreenState extends ConsumerState<TenantSelectionScreen> {
  bool _selectionEnCours = false;

  Future<void> _choisir(String tenantId) async {
    setState(() => _selectionEnCours = true);
    final succes = await ref.read(authProvider.notifier).selectionnerTenant(tenantId);
    if (!mounted) return;
    setState(() => _selectionEnCours = false);
    if (succes) {
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const KycScreen()));
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final tenants = ref.watch(tenantSelectionProvider).tenants;
    final authState = ref.watch(authProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.choisirUnBureau), automaticallyImplyLeading: false),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(t.compteMultiBureau, style: const TextStyle(color: AppColors.texteMuet, fontSize: 13)),
            const SizedBox(height: 16),
            if (authState.erreur != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.erreur.withValues(alpha: 0.08),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
                ),
                child: Text(authState.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 12)),
              ),
              const SizedBox(height: 16),
            ],
            ...tenants.map((tenant) => Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: InkWell(
                    onTap: _selectionEnCours ? null : () => _choisir(tenant.id),
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: AppColors.surface,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: AppColors.bordure),
                      ),
                      child: Row(children: [
                        const Icon(Icons.apartment_outlined, color: AppColors.accent),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            tenant.origine ? '${tenant.id} ${t.bureauPrincipal}' : tenant.id,
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                          ),
                        ),
                        if (_selectionEnCours)
                          const SizedBox(height: 18, width: 18,
                              child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.accent))
                        else
                          const Icon(Icons.chevron_right, color: AppColors.texteMuet),
                      ]),
                    ),
                  ),
                )),
          ],
        ),
      ),
    );
  }
}
