import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/auth_provider.dart';
import '../providers/choix_paiement_provider.dart';
import '../theme/app_theme.dart';

// S8 — l'écran existe, mais la logique de paiement (grand livre miroir,
// séquestre) appartient à service-pay, module de Personne 2 (Web), pas
// construit ici. Pas de fausse logique financière — état d'attente honnête.
//
// S14 (Sprint 14, "Paiements Mobile Money étendus") — branché sur le vrai
// backend (service-pay, appelé directement, voir choix_paiement_provider.dart).
// Sélecteur de moyen de règlement (MoMo / Orange Money / Espèces) rattaché
// à une mission précise.
class PaiementScreen extends ConsumerWidget {
  final String missionId;

  const PaiementScreen({super.key, required this.missionId});

  String _libelle(AppLocalizations t, MoyenPaiement moyen) =>
      moyen == MoyenPaiement.especes ? t.especes : libellesMoyenPaiement[moyen]!;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(choixPaiementProvider);
    final tenantId = ref.watch(authProvider).tenantId;
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.paiement)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
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
                child: Row(children: [
                  const Icon(Icons.info_outline, color: AppColors.accent, size: 16),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      t.intentionReglementInfo,
                      style: const TextStyle(color: AppColors.accent, fontSize: 12),
                    ),
                  ),
                ]),
              ),
              const SizedBox(height: 24),
              Text(t.choisirMoyenReglement, style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 16),
              for (final moyen in MoyenPaiement.values) ...[
                _carteMoyen(context, ref, t, moyen, state.moyenSelectionne == moyen),
                const SizedBox(height: 10),
              ],
              const SizedBox(height: 12),
              if (state.erreur != null) ...[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.erreur.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
                  ),
                  child: Row(children: [
                    const Icon(Icons.error_outline, color: AppColors.erreur, size: 18),
                    const SizedBox(width: 8),
                    Expanded(child: Text(state.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 12))),
                  ]),
                ),
                const SizedBox(height: 12),
              ],
              if (state.confirme)
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppColors.succes.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: AppColors.succes.withValues(alpha: 0.4)),
                  ),
                  child: Row(children: [
                    const Icon(Icons.check_circle, color: AppColors.succes),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        t.moyenReglementRetenu(_libelle(t, state.moyenSelectionne!)),
                        style: const TextStyle(color: AppColors.succes),
                      ),
                    ),
                  ]),
                )
              else
                SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: ElevatedButton(
                    onPressed: state.moyenSelectionne == null || state.chargement
                        ? null
                        : () => ref.read(choixPaiementProvider.notifier).confirmer(missionId, tenantId),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.accent,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                    child: state.chargement
                        ? const SizedBox(
                            width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.texteBouton))
                        : Text(t.confirmer, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _carteMoyen(BuildContext context, WidgetRef ref, AppLocalizations t, MoyenPaiement moyen, bool selectionne) {
    return InkWell(
      onTap: () => ref.read(choixPaiementProvider.notifier).selectionner(moyen),
      borderRadius: BorderRadius.circular(10),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: selectionne ? AppColors.surfaceClaire : AppColors.surface,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: selectionne ? AppColors.accent : AppColors.bordure, width: selectionne ? 1.5 : 1),
        ),
        child: Row(children: [
          Icon(
            selectionne ? Icons.radio_button_checked : Icons.radio_button_unchecked,
            color: selectionne ? AppColors.accent : AppColors.texteMuet,
          ),
          const SizedBox(width: 12),
          Text(_libelle(t, moyen),
              style: TextStyle(fontWeight: selectionne ? FontWeight.bold : FontWeight.normal, fontSize: 14)),
        ]),
      ),
    );
  }
}
