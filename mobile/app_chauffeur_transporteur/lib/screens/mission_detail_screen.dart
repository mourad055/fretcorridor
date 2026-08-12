import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/mission_provider.dart';
import '../providers/position_provider.dart';
import '../theme/app_theme.dart';

const _libellesStatut = {
  'EN_ATTENTE': 'En attente',
  'PRISE_EN_CHARGE': 'Prise en charge',
  'EN_TRANSIT': 'En transit',
  'LIVREE': 'Livrée',
  'ANNULEE': 'Annulée',
};

const _libellesEtape = {
  'PRISE_EN_CHARGE': 'Prise en charge',
  'EN_TRANSIT': 'En transit',
  'LIVRAISON': 'Livraison',
  'INCIDENT': 'Incident',
};

// S7 : détail + progression d'une mission (prise en charge → en transit →
// livraison, ou incident). Démarre/arrête automatiquement le suivi GPS (S6)
// selon le statut — c'est l'intégration prévue dès position_provider.dart.
class MissionDetailScreen extends ConsumerStatefulWidget {
  final Mission mission;
  const MissionDetailScreen({super.key, required this.mission});

  @override
  ConsumerState<MissionDetailScreen> createState() => _MissionDetailScreenState();
}

class _MissionDetailScreenState extends ConsumerState<MissionDetailScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(missionProvider.notifier).chargerDetail(widget.mission.missionId));
  }

  Future<void> _avancer(String typeEtape, String libelle) async {
    final succes = await ref.read(missionProvider.notifier).ajouterEtape(widget.mission.missionId, typeEtape, libelle);
    if (!succes || !mounted) return;

    final positionNotifier = ref.read(positionProvider.notifier);
    if (typeEtape == 'PRISE_EN_CHARGE' || typeEtape == 'EN_TRANSIT') {
      positionNotifier.demarrerSuivi(widget.mission.missionId);
    } else if (typeEtape == 'LIVRAISON') {
      positionNotifier.arreterSuivi();
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(missionProvider);
    final detail = state.detail;
    final positionState = ref.watch(positionProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text('${widget.mission.origineNom ?? '—'} → ${widget.mission.destinationNom ?? '—'}')),
      body: state.chargement && detail == null
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (positionState.suiviActif)
                    Container(
                      margin: const EdgeInsets.only(bottom: 16),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.succes.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: AppColors.succes.withValues(alpha: 0.4)),
                      ),
                      child: const Row(children: [
                        Icon(Icons.gps_fixed, color: AppColors.succes, size: 18),
                        SizedBox(width: 8),
                        Text('Suivi GPS actif', style: TextStyle(color: AppColors.succes, fontSize: 13)),
                      ]),
                    ),
                  if (state.erreur != null)
                    Container(
                      margin: const EdgeInsets.only(bottom: 16),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.erreur.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
                      ),
                      child: Row(children: [
                        const Icon(Icons.warning_amber, color: AppColors.erreur, size: 18),
                        const SizedBox(width: 8),
                        Expanded(child: Text(state.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 13))),
                      ]),
                    ),

                  Text('Statut : ${_libellesStatut[detail?.statut ?? widget.mission.statut] ?? widget.mission.statut}',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 20),

                  if (detail != null) _actions(detail, state.chargement),
                  const SizedBox(height: 24),

                  Text('Chronologie', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 12),
                  if (detail == null || detail.etapes.isEmpty)
                    const Text('Aucune étape pour le moment.', style: TextStyle(color: AppColors.texteMuet))
                  else
                    ...detail.etapes.map(_carteEtape),
                ],
              ),
            ),
    );
  }

  Widget _actions(MissionDetail detail, bool chargement) {
    final Map<String, String> suivantes = switch (detail.statut) {
      'EN_ATTENTE' => {'PRISE_EN_CHARGE': 'Prise en charge'},
      'PRISE_EN_CHARGE' => {'EN_TRANSIT': 'En transit'},
      'EN_TRANSIT' => {'LIVRAISON': 'Confirmer la livraison'},
      _ => {},
    };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (final entry in suivantes.entries) ...[
          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton(
              onPressed: chargement ? null : () => _avancer(entry.key, entry.value),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: Text(entry.value, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
            ),
          ),
          const SizedBox(height: 10),
        ],
        if (detail.statut != 'LIVREE' && detail.statut != 'ANNULEE')
          SizedBox(
            width: double.infinity,
            height: 44,
            child: OutlinedButton(
              onPressed: chargement ? null : () => _avancer('INCIDENT', 'Incident signalé par le chauffeur'),
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: AppColors.erreur),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: const Text('Signaler un incident', style: TextStyle(color: AppColors.erreur)),
            ),
          ),
      ],
    );
  }

  Widget _carteEtape(Etape etape) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Row(children: [
        Icon(etape.type == 'INCIDENT' ? Icons.warning_amber : Icons.check_circle,
            color: etape.type == 'INCIDENT' ? AppColors.erreur : AppColors.succes, size: 18),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(_libellesEtape[etape.type] ?? etape.type, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              Text(etape.libelle, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
            ],
          ),
        ),
      ]),
    );
  }
}
