import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/tenant_selection_provider.dart';
import '../theme/app_theme.dart';
import 'kyc_screen.dart';

/// S18 (Sprint 18, "Second tenant institutionnel") — ⚠️ MOCK, voir
/// tenant_selection_provider.dart. Affiché uniquement après authentification
/// réussie, avant l'accès à l'app, et seulement si le compte est rattaché à
/// plusieurs tenants (démo) — le cas normal (mono-tenant) ne passe jamais
/// par cet écran.
class TenantSelectionScreen extends ConsumerWidget {
  const TenantSelectionScreen({super.key});

  void _choisir(BuildContext context, WidgetRef ref, String tenantId) {
    ref.read(tenantSelectionProvider.notifier).selectionner(tenantId);
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const KycScreen()));
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tenants = ref.watch(tenantSelectionProvider).tenants;

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Choisir un bureau'), automaticallyImplyLeading: false),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: AppColors.accent.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.accent.withValues(alpha: 0.4)),
              ),
              child: const Row(children: [
                Icon(Icons.science_outlined, color: AppColors.accent, size: 16),
                SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Démonstration — sélection simulée en attendant le multi-tenant côté service-ida.',
                    style: TextStyle(color: AppColors.accent, fontSize: 12),
                  ),
                ),
              ]),
            ),
            const SizedBox(height: 20),
            const Text('Votre compte est rattaché à plusieurs bureaux. Choisissez celui avec lequel travailler :',
                style: TextStyle(color: AppColors.texteMuet, fontSize: 13)),
            const SizedBox(height: 16),
            ...tenants.map((tenant) => Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: InkWell(
                    onTap: () => _choisir(context, ref, tenant.id),
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
                        Expanded(child: Text(tenant.nom, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14))),
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
