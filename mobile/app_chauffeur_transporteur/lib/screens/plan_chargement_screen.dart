import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/plan_chargement_provider.dart';
import '../theme/app_theme.dart';

const _libellesOrientation = {
  OrientationColis.debout: 'Debout',
  OrientationColis.couche: 'Couché',
  OrientationColis.fragileDessus: 'Fragile — dessus',
};

const _iconesOrientation = {
  OrientationColis.debout: Icons.arrow_upward,
  OrientationColis.couche: Icons.arrow_forward,
  OrientationColis.fragileDessus: Icons.warning_amber,
};

/// S16 (Sprint 16, "Oracle de chargement 3D") — ⚠️ MOCK, voir
/// plan_chargement_provider.dart. Restitution visuelle en lecture seule du
/// plan de chargement calculé par le Moteur (positions des colis dans le
/// véhicule, orientations, répartition des charges par essieu) —
/// accessible depuis une mission simple (S7) ou une étape de tournée (S11).
class PlanChargementScreen extends ConsumerStatefulWidget {
  final String missionId;
  final String? etapeLibelle;

  const PlanChargementScreen({super.key, required this.missionId, this.etapeLibelle});

  @override
  ConsumerState<PlanChargementScreen> createState() => _PlanChargementScreenState();
}

class _PlanChargementScreenState extends ConsumerState<PlanChargementScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() =>
        ref.read(planChargementProvider.notifier).chargerPlanDemo(widget.missionId, etapeLibelle: widget.etapeLibelle));
  }

  @override
  Widget build(BuildContext context) {
    final plan = ref.watch(planChargementProvider).plan;

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Plan de chargement')),
      body: plan == null
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
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
                          'Démonstration — plan simulé en attendant l\'oracle de chargement 3D côté service-opt V2.',
                          style: TextStyle(color: AppColors.accent, fontSize: 12),
                        ),
                      ),
                    ]),
                  ),
                  const SizedBox(height: 20),
                  if (plan.etapeLibelle != null) ...[
                    Text('Étape : ${plan.etapeLibelle}', style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                    const SizedBox(height: 12),
                  ],
                  Text('Répartition par essieu', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 12),
                  ...plan.essieux.map(_carteEssieu),
                  const SizedBox(height: 24),
                  Text('Colis dans le véhicule (avant → arrière)', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 12),
                  ...plan.colis.map(_carteColis),
                ],
              ),
            ),
    );
  }

  Widget _carteEssieu(EssieuCharge essieu) {
    final ratio = (essieu.poidsKg / essieu.poidsMaxKg).clamp(0.0, 1.0);
    final couleur = essieu.surcharge ? AppColors.erreur : AppColors.succes;
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(essieu.libelle, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              Text('${essieu.poidsKg.round()} / ${essieu.poidsMaxKg.round()} kg',
                  style: TextStyle(color: couleur, fontSize: 12, fontWeight: FontWeight.w600)),
            ],
          ),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: ratio,
              minHeight: 8,
              backgroundColor: AppColors.bordure,
              valueColor: AlwaysStoppedAnimation(couleur),
            ),
          ),
          if (essieu.surcharge) ...[
            const SizedBox(height: 6),
            const Text('Surcharge sur cet essieu', style: TextStyle(color: AppColors.erreur, fontSize: 11)),
          ],
        ],
      ),
    );
  }

  Widget _carteColis(ColisPlan colis) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Row(children: [
        Container(
          width: 28,
          height: 28,
          alignment: Alignment.center,
          decoration: BoxDecoration(color: AppColors.surfaceClaire, borderRadius: BorderRadius.circular(14)),
          child: Text('${colis.position}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.accent)),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(colis.libelle, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              Text('${colis.poidsKg.round()} kg', style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
            ],
          ),
        ),
        Icon(_iconesOrientation[colis.orientation], color: AppColors.texteMuet, size: 16),
        const SizedBox(width: 4),
        Text(_libellesOrientation[colis.orientation]!, style: const TextStyle(color: AppColors.texteMuet, fontSize: 11)),
      ]),
    );
  }
}
