import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/axes_provider.dart';
import '../providers/tarif_axe_provider.dart';
import '../theme/app_theme.dart';

// Tarification par chauffeur (spec-tarification-par-chauffeur.md) : chaque
// chauffeur regle lui-meme 3 tarifs en F/kg (standard/fragile/ultra fragile)
// pour chaque axe du referentiel ou il souhaite recevoir des demandes. Les
// axes eux-memes viennent du vrai referentiel geo.axe (axesProvider, deja
// branche sur GET /axes) -- on ne permet pas d'en creer un ici, seulement
// de choisir parmi ceux qui existent. Un axe est a sens unique : le retour,
// s'il existe dans le referentiel, est un axe distinct a configurer a part.
class MesTarifsScreen extends ConsumerStatefulWidget {
  const MesTarifsScreen({super.key});

  @override
  ConsumerState<MesTarifsScreen> createState() => _MesTarifsScreenState();
}

class _MesTarifsScreenState extends ConsumerState<MesTarifsScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(axesProvider.notifier).charger();
      ref.read(tarifAxeProvider.notifier).charger();
    });
  }

  @override
  Widget build(BuildContext context) {
    final axesState = ref.watch(axesProvider);
    final tarifState = ref.watch(tarifAxeProvider);
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.mesTarifs)),
      body: RefreshIndicator(
        onRefresh: () => Future.wait([
          ref.read(axesProvider.notifier).charger(),
          ref.read(tarifAxeProvider.notifier).charger(),
        ]),
        child: axesState.chargement && axesState.axes.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : axesState.erreur != null
                ? _erreur(axesState.erreur!)
                : axesState.axes.isEmpty
                    ? ListView(children: [
                        const SizedBox(height: 80),
                        Center(child: Text(t.aucunAxeConfigure, style: const TextStyle(color: AppColors.texteMuet))),
                      ])
                    : ListView(
                        padding: const EdgeInsets.all(16),
                        children: [
                          Container(
                            padding: const EdgeInsets.all(14),
                            margin: const EdgeInsets.only(bottom: 12),
                            decoration: BoxDecoration(
                              color: AppColors.surface,
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(color: AppColors.bordure),
                            ),
                            child: Text(t.mesTarifsIntro, style: const TextStyle(color: AppColors.texteMuet, fontSize: 13)),
                          ),
                          for (final axe in axesState.axes) ...[
                            _carteAxe(t, axe, tarifState.tarifsParAxeId[axe.id]),
                            const SizedBox(height: 12),
                          ],
                        ],
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

  Widget _carteAxe(AppLocalizations t, Axe axe, TarifAxe? tarif) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            Expanded(
              child: Text('${axe.origine} → ${axe.destination}',
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
            ),
            Text('${axe.distanceKm.round()} km', style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
          ]),
          if (tarif != null) ...[
            const SizedBox(height: 14),
            for (final categorie in CategorieMarchandise.values) ...[
              _ligneTarif(t, axe.id, categorie, tarif.tarifsParKg[categorie] ?? 0),
              const SizedBox(height: 8),
            ],
            const SizedBox(height: 4),
            Row(children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => ref.read(tarifAxeProvider.notifier).toggleActif(axe.id),
                  icon: Icon(tarif.actif ? Icons.pause_circle_outline : Icons.play_circle_outline, size: 18),
                  label: Text(tarif.actif ? t.axeEnPause : t.axeActif),
                  style: OutlinedButton.styleFrom(foregroundColor: AppColors.texte),
                ),
              ),
              const SizedBox(width: 10),
              OutlinedButton(
                onPressed: () => ref.read(tarifAxeProvider.notifier).retirer(axe.id),
                style: OutlinedButton.styleFrom(foregroundColor: AppColors.erreur, side: const BorderSide(color: AppColors.erreur)),
                child: Text(t.retirerAxe),
              ),
            ]),
          ] else ...[
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () => ref.read(tarifAxeProvider.notifier).configurer(axe.id),
                style: ElevatedButton.styleFrom(backgroundColor: AppColors.accent),
                child: Text(t.configurerTarifs, style: const TextStyle(color: AppColors.texteBouton)),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _ligneTarif(AppLocalizations t, String axeId, CategorieMarchandise categorie, double valeur) {
    final libelle = switch (categorie) {
      CategorieMarchandise.standard => t.categorieMarchandiseStandard,
      CategorieMarchandise.fragile => t.categorieMarchandiseFragile,
      CategorieMarchandise.ultraFragile => t.categorieMarchandiseUltraFragile,
    };
    return Row(children: [
      Expanded(child: Text(libelle, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600))),
      IconButton(
        onPressed: () => ref.read(tarifAxeProvider.notifier).ajusterTarif(axeId, categorie, -1),
        icon: const Icon(Icons.remove_circle_outline, size: 20),
        color: AppColors.texteMuet,
        constraints: const BoxConstraints(),
        padding: const EdgeInsets.all(6),
      ),
      SizedBox(
        width: 64,
        child: Text('${valeur.round()} ${t.tarifParKg}',
            textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
      ),
      IconButton(
        onPressed: () => ref.read(tarifAxeProvider.notifier).ajusterTarif(axeId, categorie, 1),
        icon: const Icon(Icons.add_circle_outline, size: 20),
        color: AppColors.accent,
        constraints: const BoxConstraints(),
        padding: const EdgeInsets.all(6),
      ),
    ]);
  }
}
