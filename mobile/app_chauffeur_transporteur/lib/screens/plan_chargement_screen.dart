import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/plan_chargement_provider.dart';
import '../theme/app_theme.dart';

Map<String, String> _libellesTypeEtape(AppLocalizations t) => {
      'ENLEVEMENT': t.enlevementLabel,
      'LIVRAISON': t.etapeLivraison,
    };

/// S16 (Sprint 16, "Oracle de chargement 3D") — appel réel depuis le
/// 23 août, voir plan_chargement_provider.dart. Restitution en lecture
/// seule de la répartition de poids par essieu calculée par le Moteur pour
/// chaque état de la tournée — accessible depuis une tournée multi-étapes
/// (S11) ou le détail d'une mission qui en fait partie.
///
/// N'affiche PAS de positions/orientations de colis dans le véhicule : le
/// Moteur ne les calcule pas (contrat colis 3D pas encore défini), les
/// inventer serait trompeur pour le chauffeur.
class PlanChargementScreen extends ConsumerStatefulWidget {
  final String tourneeId;
  final String? missionIdFiltre;

  const PlanChargementScreen({super.key, required this.tourneeId, this.missionIdFiltre});

  @override
  ConsumerState<PlanChargementScreen> createState() => _PlanChargementScreenState();
}

class _PlanChargementScreenState extends ConsumerState<PlanChargementScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() =>
        ref.read(planChargementProvider.notifier).charger(widget.tourneeId, missionIdFiltre: widget.missionIdFiltre));
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(planChargementProvider);
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.planDeChargementTitre)),
      body: state.chargement
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (state.erreur != null)
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
                    )
                  else if (state.etapes.isEmpty)
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceClaire,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: AppColors.bordure),
                      ),
                      child: Row(children: [
                        const Icon(Icons.hourglass_empty, color: AppColors.texteMuet, size: 18),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            t.planChargementNonDisponibleMessage,
                            style: const TextStyle(color: AppColors.texteMuet, fontSize: 13),
                          ),
                        ),
                      ]),
                    )
                  else
                    ...state.etapes.map((e) => _carteEtape(t, e)),
                ],
              ),
            ),
    );
  }

  Widget _carteEtape(AppLocalizations t, EtapePlanChargement etape) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            t.etapePlanLabel(etape.rang, _libellesTypeEtape(t)[etape.typeEtape] ?? etape.typeEtape, etape.demandeId.substring(0, 8)),
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
          ),
          const SizedBox(height: 4),
          Text(
            t.repartitionApproximativeMessage,
            style: const TextStyle(color: AppColors.texteMuet, fontSize: 11),
          ),
          const SizedBox(height: 10),
          ...etape.essieux.map((e) => Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(e.libelle, style: const TextStyle(fontSize: 12)),
                    Text('${e.poidsKg.round()} kg', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
                  ],
                ),
              )),
        ],
      ),
    );
  }
}
