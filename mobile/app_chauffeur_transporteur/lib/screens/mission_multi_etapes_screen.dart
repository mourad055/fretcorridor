import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/mission_multi_etapes_provider.dart';
import '../theme/app_theme.dart';
import 'plan_chargement_screen.dart';

const _libellesType = {
  TypeEtapeTournee.enlevement: 'Enlèvement',
  TypeEtapeTournee.livraison: 'Livraison',
};

/// S11 — écran de tournée multi-étapes (groupage LTL), câblé sur
/// GET /missions/tournees/{tourneeId} (voir mission_multi_etapes_provider.dart).
/// N'affiche qu'UNE seule action à la fois (l'étape en cours), avec la
/// chronologie des étapes déjà faites en dessous — même principe que
/// l'écran S7 (mission_detail_screen.dart), généralisé à N étapes au lieu
/// d'un statut linéaire fixe. Écran séparé, le flux S7 existant n'est pas
/// modifié.
class MissionMultiEtapesScreen extends ConsumerStatefulWidget {
  final String tourneeId;

  const MissionMultiEtapesScreen({super.key, required this.tourneeId});

  @override
  ConsumerState<MissionMultiEtapesScreen> createState() => _MissionMultiEtapesScreenState();
}

class _MissionMultiEtapesScreenState extends ConsumerState<MissionMultiEtapesScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(missionMultiEtapesProvider.notifier).chargerTournee(widget.tourneeId));
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(missionMultiEtapesProvider);
    final tournee = state.tournee;

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Tournée groupée')),
      body: RefreshIndicator(
        onRefresh: () => ref.read(missionMultiEtapesProvider.notifier).chargerTournee(widget.tourneeId),
        child: state.chargement && tournee == null
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null && tournee == null
                ? _erreur(state.erreur!)
                : tournee == null
                    ? const SizedBox.shrink()
                    : SingleChildScrollView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        padding: const EdgeInsets.all(20),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            if (state.erreur != null) ...[
                              _bandeauErreur(state.erreur!),
                              const SizedBox(height: 16),
                            ],
                            Text('Envoi groupé — ${tournee.etapes.length} étapes',
                                style: Theme.of(context).textTheme.titleMedium),
                            const SizedBox(height: 16),
                            if (tournee.terminee) _carteTourneeTerminee() else _carteEtapeCourante(tournee.etapeCourante!, state.chargement),
                            const SizedBox(height: 24),
                            Text('Chronologie', style: Theme.of(context).textTheme.titleMedium),
                            const SizedBox(height: 12),
                            if (tournee.etapesTerminees.isEmpty)
                              const Text('Aucune étape terminée pour le moment.', style: TextStyle(color: AppColors.texteMuet))
                            else
                              ...tournee.etapesTerminees.map(_carteEtapeTerminee),
                          ],
                        ),
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

  Widget _bandeauErreur(String message) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: AppColors.erreur.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
      ),
      child: Row(children: [
        const Icon(Icons.error_outline, color: AppColors.erreur, size: 16),
        const SizedBox(width: 8),
        Expanded(child: Text(message, style: const TextStyle(color: AppColors.erreur, fontSize: 12))),
      ]),
    );
  }

  Widget _carteEtapeCourante(EtapeTournee etape, bool chargement) {
    final estEnlevement = etape.type == TypeEtapeTournee.enlevement;
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
            Icon(estEnlevement ? Icons.inventory_2_outlined : Icons.flag_outlined, color: AppColors.accent),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(_libellesType[etape.type]!, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                  Text('Demande ${etape.demandeId.substring(0, 8)}',
                      style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                  Text('${etape.pointLatitude.toStringAsFixed(4)}, ${etape.pointLongitude.toStringAsFixed(4)}',
                      style: const TextStyle(color: AppColors.texteMuet, fontSize: 11)),
                ],
              ),
            ),
          ]),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton(
              onPressed: chargement ? null : () => ref.read(missionMultiEtapesProvider.notifier).confirmerEtapeCourante(),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: chargement
                  ? const SizedBox(
                      width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.texteBouton))
                  : Text(estEnlevement ? 'Confirmer l\'enlèvement' : 'Confirmer la livraison',
                      style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
            ),
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            height: 40,
            child: OutlinedButton.icon(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => PlanChargementScreen(
                      missionId: etape.missionId,
                      etapeLibelle: '${_libellesType[etape.type]} — Demande ${etape.demandeId.substring(0, 8)}',
                    ),
                  ),
                );
              },
              icon: const Icon(Icons.view_in_ar_outlined, size: 16),
              label: const Text('Voir le plan de chargement', style: TextStyle(fontSize: 13)),
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.texte,
                side: const BorderSide(color: AppColors.bordure),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _carteTourneeTerminee() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.succes.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.succes.withValues(alpha: 0.4)),
      ),
      child: const Row(children: [
        Icon(Icons.check_circle, color: AppColors.succes),
        SizedBox(width: 10),
        Expanded(child: Text('Toutes les étapes de la tournée sont terminées.', style: TextStyle(color: AppColors.succes))),
      ]),
    );
  }

  Widget _carteEtapeTerminee(EtapeTournee etape) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Row(children: [
        const Icon(Icons.check_circle, color: AppColors.succes, size: 18),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(_libellesType[etape.type]!, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              Text('Demande ${etape.demandeId.substring(0, 8)}', style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
            ],
          ),
        ),
      ]),
    );
  }
}
