import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/axes_provider.dart';
import '../providers/capacite_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/top_notification.dart';
import 'capacite_screen.dart';

// "Mes capacités" — liste des déclarations du transporteur connecté (date,
// heure, statut) avec suppression, absente jusqu'ici (seule la dernière
// déclaration de la session en cours était visible, rien de persistant).
class MesCapacitesScreen extends ConsumerStatefulWidget {
  const MesCapacitesScreen({super.key});

  @override
  ConsumerState<MesCapacitesScreen> createState() => _MesCapacitesScreenState();
}

class _MesCapacitesScreenState extends ConsumerState<MesCapacitesScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(capaciteProvider.notifier).chargerMesCapacites();
      ref.read(axesProvider.notifier).charger();
    });
  }

  Future<void> _supprimer(CapaciteDeclaree capacite) async {
    final confirme = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Supprimer cette capacité ?'),
        content: const Text('Cette action est définitive.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Supprimer', style: TextStyle(color: AppColors.erreur))),
        ],
      ),
    );
    if (confirme != true) return;
    final succes = await ref.read(capaciteProvider.notifier).supprimer(capacite.id);
    if (!mounted) return;
    if (succes) {
      afficherNotification(context, message: 'Capacité supprimée.', couleur: AppColors.succes, icone: Icons.check_circle);
    } else {
      afficherNotification(context, message: 'Suppression impossible pour le moment.', couleur: AppColors.erreur, icone: Icons.error_outline);
    }
  }

  String _formatDate(DateTime? d) {
    if (d == null) return '—';
    final jours = ['lun.', 'mar.', 'mer.', 'jeu.', 'ven.', 'sam.', 'dim.'];
    final j = jours[d.weekday - 1];
    final heure = d.hour.toString().padLeft(2, '0');
    final minute = d.minute.toString().padLeft(2, '0');
    return '$j ${d.day}/${d.month}/${d.year} à $heure:$minute';
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(capaciteProvider);
    final axes = ref.watch(axesProvider).axes;

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Mes capacités')),
      body: RefreshIndicator(
        onRefresh: () => ref.read(capaciteProvider.notifier).chargerMesCapacites(),
        child: state.chargement && state.mesCapacites.isEmpty
            ? const Center(child: CircularProgressIndicator(color: AppColors.accent))
            : state.mesCapacites.isEmpty
                ? ListView(
                    children: [
                      const SizedBox(height: 100),
                      const Icon(Icons.local_shipping_outlined, color: AppColors.bordure, size: 48),
                      const SizedBox(height: 12),
                      const Center(child: Text('Aucune capacité déclarée pour le moment',
                          style: TextStyle(color: AppColors.texteMuet, fontWeight: FontWeight.bold))),
                    ],
                  )
                : ListView.builder(
                    padding: const EdgeInsets.all(20),
                    itemCount: state.mesCapacites.length,
                    itemBuilder: (context, i) {
                      final c = state.mesCapacites[i];
                      final statutCouleur = c.expiree ? AppColors.texteMuet : (c.publiee ? AppColors.succes : AppColors.accent);
                      final statutLibelle = c.expiree ? 'Expirée' : (c.publiee ? 'Publiée' : 'En attente');
                      Axe? axe;
                      for (final a in axes) {
                        if (a.id == c.axeId) { axe = a; break; }
                      }
                      return Container(
                        margin: const EdgeInsets.only(bottom: 12),
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: AppColors.surface,
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(color: AppColors.bordure),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      if (axe != null)
                                        Text('${axe.origine} → ${axe.destination}',
                                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppColors.accent)),
                                      Text('${c.poidsKg.toStringAsFixed(0)} kg disponibles',
                                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                                    ],
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                  decoration: BoxDecoration(color: statutCouleur.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
                                  child: Text(statutLibelle, style: TextStyle(color: statutCouleur, fontSize: 11, fontWeight: FontWeight.bold)),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Row(children: [
                              const Icon(Icons.schedule, size: 15, color: AppColors.texteMuet),
                              const SizedBox(width: 6),
                              Text('Départ : ${_formatDate(c.dateDepart)}', style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                            ]),
                            const SizedBox(height: 4),
                            Row(children: [
                              const Icon(Icons.add_circle_outline, size: 15, color: AppColors.texteMuet),
                              const SizedBox(width: 6),
                              Text('Déclarée le ${_formatDate(c.dateCreation)}', style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                            ]),
                            const SizedBox(height: 12),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                if (!c.expiree)
                                  TextButton.icon(
                                    onPressed: () async {
                                      await Navigator.push(
                                        context,
                                        MaterialPageRoute(builder: (_) => CapaciteScreen(capaciteAModifier: c)),
                                      );
                                      if (context.mounted) ref.read(capaciteProvider.notifier).chargerMesCapacites();
                                    },
                                    icon: const Icon(Icons.edit_outlined, color: AppColors.accent, size: 18),
                                    label: const Text('Modifier', style: TextStyle(color: AppColors.accent)),
                                  ),
                                TextButton.icon(
                                  onPressed: () => _supprimer(c),
                                  icon: const Icon(Icons.delete_outline, color: AppColors.erreur, size: 18),
                                  label: const Text('Supprimer', style: TextStyle(color: AppColors.erreur)),
                                ),
                              ],
                            ),
                          ],
                        ),
                      );
                    },
                  ),
      ),
    );
  }
}
