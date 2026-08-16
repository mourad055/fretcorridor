import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/choix_paiement_provider.dart';
import '../theme/app_theme.dart';

// S8 — l'écran existe, mais la logique de paiement (grand livre miroir,
// séquestre) appartient à service-pay, module de Personne 2 (Web), pas
// construit ici. Pas de fausse logique financière — état d'attente honnête.
//
// S14 (Sprint 14, "Paiements Mobile Money étendus") — ⚠️ MOCK, voir
// choix_paiement_provider.dart. Sélecteur de moyen de règlement (MoMo /
// Orange Money / Espèces) construit dès maintenant pour valider l'écran ;
// la confirmation reste entièrement locale, aucun appel à service-pay.
class PaiementScreen extends ConsumerWidget {
  const PaiementScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(choixPaiementProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Paiement')),
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
                child: const Row(children: [
                  Icon(Icons.science_outlined, color: AppColors.accent, size: 16),
                  SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Démonstration — le paiement s\'effectuera via un prestataire agréé '
                      'dès que cette étape sera finalisée côté service-pay.',
                      style: TextStyle(color: AppColors.accent, fontSize: 12),
                    ),
                  ),
                ]),
              ),
              const SizedBox(height: 24),
              Text('Choisissez votre moyen de règlement', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 16),
              for (final moyen in MoyenPaiement.values) ...[
                _carteMoyen(context, ref, moyen, state.moyenSelectionne == moyen),
                const SizedBox(height: 10),
              ],
              const SizedBox(height: 12),
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
                        'Moyen de règlement retenu : ${libellesMoyenPaiement[state.moyenSelectionne]}.',
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
                    onPressed: state.moyenSelectionne == null
                        ? null
                        : () => ref.read(choixPaiementProvider.notifier).confirmerMock(),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.accent,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                    child: const Text('Confirmer', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _carteMoyen(BuildContext context, WidgetRef ref, MoyenPaiement moyen, bool selectionne) {
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
          Text(libellesMoyenPaiement[moyen]!,
              style: TextStyle(fontWeight: selectionne ? FontWeight.bold : FontWeight.normal, fontSize: 14)),
        ]),
      ),
    );
  }
}
