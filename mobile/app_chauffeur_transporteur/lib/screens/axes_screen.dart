import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/axes_provider.dart';
import '../theme/app_theme.dart';

// S3 : écran axes disponibles, verrous visibles (RG-012 — la consultation
// n'est jamais bloquée, même si matching/paiement sont encore inactifs).
//
// S15 (Sprint 15, "Second axe & sécurité") : sélecteur d'axe actif —
// permet au chauffeur de choisir parmi plusieurs axes. service-geo expose
// désormais réellement plusieurs axes par tenant (GET /axes) — plus besoin
// de second axe fictif.
class AxesScreen extends ConsumerStatefulWidget {
  const AxesScreen({super.key});

  @override
  ConsumerState<AxesScreen> createState() => _AxesScreenState();
}

class _AxesScreenState extends ConsumerState<AxesScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(axesProvider.notifier).charger());
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(axesProvider);
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.axes)),
      body: RefreshIndicator(
        onRefresh: () => ref.read(axesProvider.notifier).charger(),
        child: state.chargement && state.axes.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null
                ? _erreur(state.erreur!)
                : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: state.axes.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 10),
                    itemBuilder: (context, i) => _carteAxe(t, state.axes[i], state.axeSelectionneId),
                  ),
      ),
    );
  }

  Widget _erreur(String message) {
    return ListView(children: [
      const SizedBox(height: 80),
      const Center(child: Icon(Icons.wifi_off, color: AppColors.texteMuet, size: 48)),
      const SizedBox(height: 12),
      Center(child: Text(message, style: const TextStyle(color: AppColors.texteMuet))),
    ]);
  }

  Widget _carteAxe(AppLocalizations t, Axe axe, String? axeSelectionneId) {
    final selectionne = axe.id == axeSelectionneId;
    return InkWell(
      onTap: () => ref.read(axesProvider.notifier).selectionner(axe.id),
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: selectionne ? AppColors.surfaceClaire : AppColors.surface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: selectionne ? AppColors.accent : AppColors.bordure, width: selectionne ? 1.5 : 1),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Icon(selectionne ? Icons.radio_button_checked : Icons.radio_button_unchecked,
                  color: selectionne ? AppColors.accent : AppColors.texteMuet, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text('${axe.origine} → ${axe.destination}',
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
              ),
              Text('${axe.distanceKm.round()} km', style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
            ]),
            const SizedBox(height: 12),
            Wrap(spacing: 8, runSpacing: 8, children: [
              _badge(t.badgeVisible, axe.visibiliteActive),
              _badge(t.badgeMatching, axe.matchingActif),
              _badge(t.badgePaiement, axe.paiementActif),
            ]),
          ],
        ),
      ),
    );
  }

  Widget _badge(String label, bool actif) {
    final couleur = actif ? AppColors.succes : AppColors.texteMuet;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: couleur.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: couleur.withValues(alpha: 0.4)),
      ),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        Icon(actif ? Icons.check_circle : Icons.lock_outline, color: couleur, size: 13),
        const SizedBox(width: 4),
        Text(label, style: TextStyle(color: couleur, fontSize: 11, fontWeight: FontWeight.w600)),
      ]),
    );
  }
}
