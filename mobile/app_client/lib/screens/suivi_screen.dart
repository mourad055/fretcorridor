import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../mock/consolidation_mock.dart';
import '../providers/suivi_provider.dart';
import '../theme/app_theme.dart';
import 'litige_screen.dart';

// S6 (position/ETA) + S7 (chronologie) réunis dans un seul écran "Suivi" —
// plus naturel côté client qu'une navigation séparée pour deux vues
// étroitement liées. Reste vide tant qu'aucun chauffeur n'a été apparié
// (matching V0 encore un stub côté Moteur — voir README).
class SuiviScreen extends ConsumerStatefulWidget {
  final String demandeId;
  const SuiviScreen({super.key, required this.demandeId});

  @override
  ConsumerState<SuiviScreen> createState() => _SuiviScreenState();
}

class _SuiviScreenState extends ConsumerState<SuiviScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(suiviProvider.notifier).charger(widget.demandeId);
    });
  }

  IconData _iconePourType(String type) {
    switch (type) {
      case 'PRISE_EN_CHARGE': return Icons.local_shipping;
      case 'EN_TRANSIT': return Icons.route;
      case 'LIVRAISON': return Icons.check_circle;
      case 'INCIDENT': return Icons.warning;
      default: return Icons.circle;
    }
  }

  @override
  Widget build(BuildContext context) {
    final suivi = ref.watch(suiviProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(
        title: const Text('Suivi de ma livraison'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: AppColors.texteMuet),
            onPressed: () => ref.read(suiviProvider.notifier).charger(widget.demandeId),
          ),
        ],
      ),
      body: suivi.chargement
          ? const Center(child: CircularProgressIndicator(color: AppColors.accent))
          : suivi.chronologie == null
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.hourglass_empty, color: AppColors.bordure, size: 48),
                        const SizedBox(height: 12),
                        const Text('Suivi pas encore disponible',
                            style: TextStyle(color: AppColors.texteMuet, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 6),
                        const Text(
                          'Le suivi démarre dès qu\'un transporteur prend en charge votre demande.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: AppColors.texteMuet, fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                )
              : ListView(
                  padding: const EdgeInsets.all(20),
                  children: [
                    // S11 — indicateur "envoi consolidé" (MOCK, voir
                    // consolidation_mock.dart). Purement informatif, aucune
                    // action associée. N'affecte pas le rendu par défaut :
                    // absent quand l'envoi n'est pas (mockquement) consolidé.
                    if (estEnvoiConsolideMock(suivi.chronologie!)) ...[
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                        decoration: BoxDecoration(
                          color: AppColors.marqueOrange.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: AppColors.marqueOrange.withValues(alpha: 0.4)),
                        ),
                        child: Row(children: [
                          const Icon(Icons.merge_type, color: AppColors.marqueOrange, size: 18),
                          const SizedBox(width: 8),
                          const Expanded(
                            child: Text(
                              'Envoi groupé : votre colis fait partie d\'une tournée consolidée avec d\'autres envois.',
                              style: TextStyle(color: AppColors.marqueOrange, fontSize: 12, fontWeight: FontWeight.w600),
                            ),
                          ),
                        ]),
                      ),
                      const SizedBox(height: 16),
                    ],
                    // ── Position (S6) ──────────────────────
                    if (suivi.position != null) ...[
                      Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceClaire,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Row(children: [
                          const Icon(Icons.location_on, color: AppColors.accent),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  '${suivi.position!.latitude.toStringAsFixed(4)}, ${suivi.position!.longitude.toStringAsFixed(4)}',
                                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                                ),
                                Text(
                                  'Position mise à jour il y a ${suivi.position!.ageSecondes ~/ 60} min',
                                  style: const TextStyle(color: AppColors.texteMuet, fontSize: 11),
                                ),
                              ],
                            ),
                          ),
                        ]),
                      ),
                      const SizedBox(height: 20),
                    ] else ...[
                      Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(color: AppColors.surfaceClaire, borderRadius: BorderRadius.circular(10)),
                        child: const Text('Position GPS pas encore disponible.',
                            style: TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                      ),
                      const SizedBox(height: 20),
                    ],

                    // ── Chronologie (S7) ───────────────────
                    Text('Étapes', style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: 12),
                    if (suivi.chronologie!.etapes.isEmpty)
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 12),
                        child: Text('Aucune étape enregistrée pour le moment.',
                            style: TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                      )
                    else
                      ...suivi.chronologie!.etapes.map((e) => Padding(
                            padding: const EdgeInsets.only(bottom: 14),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Icon(_iconePourType(e.type), color: AppColors.accent, size: 20),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(e.libelle, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                                      Text(
                                        e.horodatageTransmission.toString().substring(0, 16),
                                        style: const TextStyle(color: AppColors.texteMuet, fontSize: 11),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          )),
                    const SizedBox(height: 24),
                    SizedBox(
                      width: double.infinity,
                      height: 44,
                      child: OutlinedButton.icon(
                        onPressed: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => LitigeScreen(
                              demandeId: widget.demandeId,
                              missionId: suivi.chronologie!.missionId,
                            ),
                          ),
                        ),
                        icon: const Icon(Icons.flag_outlined, size: 18, color: AppColors.erreur),
                        label: const Text('Signaler un litige', style: TextStyle(color: AppColors.erreur)),
                        style: OutlinedButton.styleFrom(
                          side: const BorderSide(color: AppColors.erreur),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                        ),
                      ),
                    ),
                  ],
                ),
    );
  }
}
