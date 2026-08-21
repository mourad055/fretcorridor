import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/mission_provider.dart';
import '../theme/app_theme.dart';
import 'mission_detail_screen.dart';
import 'mission_multi_etapes_screen.dart';

const _libellesStatut = {
  'EN_ATTENTE': 'En attente',
  'PRISE_EN_CHARGE': 'Prise en charge',
  'EN_TRANSIT': 'En transit',
  'LIVREE': 'Livrée',
  'ANNULEE': 'Annulée',
};

// S7 : liste des missions du chauffeur/transporteur connecté. Peut rester
// vide tant que le lien mission↔chauffeur n'est pas peuplé en amont côté
// service-opt (écart documenté, cf. README) — pas un bug de cet écran.
class MissionsScreen extends ConsumerStatefulWidget {
  const MissionsScreen({super.key});

  @override
  ConsumerState<MissionsScreen> createState() => _MissionsScreenState();
}

class _MissionsScreenState extends ConsumerState<MissionsScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(missionProvider.notifier).chargerMesMissions());
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(missionProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Mes missions')),
      body: RefreshIndicator(
        onRefresh: () => ref.read(missionProvider.notifier).chargerMesMissions(),
        child: state.chargement && state.missions.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null
                ? _erreur(state.erreur!)
                : state.missions.isEmpty
                    ? ListView(children: const [
                        SizedBox(height: 80),
                        Center(
                          child: Padding(
                            padding: EdgeInsets.symmetric(horizontal: 32),
                            child: Text(
                              'Aucune mission pour le moment.',
                              textAlign: TextAlign.center,
                              style: TextStyle(color: AppColors.texteMuet),
                            ),
                          ),
                        ),
                      ])
                    : ListView.separated(
                        padding: const EdgeInsets.all(16),
                        itemCount: state.missions.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 10),
                        itemBuilder: (context, i) => _carteMission(state.missions[i]),
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

  Widget _carteMission(Mission mission) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Column(
        children: [
          InkWell(
            onTap: () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => MissionDetailScreen(mission: mission)));
            },
            borderRadius: BorderRadius.circular(12),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(children: [
                const Icon(Icons.local_shipping_outlined, color: AppColors.accent),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${mission.origineNom ?? '—'} → ${mission.destinationNom ?? '—'}',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                      if (mission.typeEmballageNom != null) ...[
                        const SizedBox(height: 2),
                        Text(
                          '${mission.quantite ?? ''} × ${mission.typeEmballageNom}'
                          '${mission.poidsTaxableKg != null ? ' — ${mission.poidsTaxableKg!.toStringAsFixed(0)} kg' : ''}',
                          style: const TextStyle(color: AppColors.texteMuet, fontSize: 12),
                        ),
                      ],
                      const SizedBox(height: 4),
                      Text(_libellesStatut[mission.statut] ?? mission.statut,
                          style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right, color: AppColors.texteMuet),
              ]),
            ),
          ),
          // S11 (EF-MAT-05/06) : présent uniquement si service-opt a
          // regroupé cette Mission dans une Tournée consolidée (LTL).
          if (mission.tourneeId != null)
            InkWell(
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => MissionMultiEtapesScreen(tourneeId: mission.tourneeId!)),
                );
              },
              borderRadius: const BorderRadius.vertical(bottom: Radius.circular(12)),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                decoration: BoxDecoration(
                  color: AppColors.accent.withValues(alpha: 0.08),
                  borderRadius: const BorderRadius.vertical(bottom: Radius.circular(12)),
                  border: Border(top: BorderSide(color: AppColors.bordure)),
                ),
                child: const Row(children: [
                  Icon(Icons.alt_route_outlined, color: AppColors.accent, size: 16),
                  SizedBox(width: 8),
                  Text('Fait partie d\'une tournée groupée', style: TextStyle(color: AppColors.accent, fontSize: 12)),
                  Spacer(),
                  Icon(Icons.chevron_right, color: AppColors.accent, size: 16),
                ]),
              ),
            ),
        ],
      ),
    );
  }
}
