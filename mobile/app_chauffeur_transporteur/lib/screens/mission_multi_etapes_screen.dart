import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/mission_multi_etapes_provider.dart';
import '../theme/app_theme.dart';

const _libellesType = {
  TypeEtapeTournee.enlevement: 'Enlèvement',
  TypeEtapeTournee.livraison: 'Livraison',
};

/// S11 — écran de tournée multi-étapes (groupage LTL) — ⚠️ MOCK, voir
/// mission_multi_etapes_provider.dart. N'affiche qu'UNE seule action à la
/// fois (l'étape en cours), avec la chronologie des étapes déjà faites en
/// dessous — même principe que l'écran S7 (mission_detail_screen.dart),
/// généralisé à N étapes au lieu d'un statut linéaire fixe. Écran séparé,
/// le flux S7 existant n'est pas modifié.
class MissionMultiEtapesScreen extends ConsumerStatefulWidget {
  const MissionMultiEtapesScreen({super.key});

  @override
  ConsumerState<MissionMultiEtapesScreen> createState() => _MissionMultiEtapesScreenState();
}

class _MissionMultiEtapesScreenState extends ConsumerState<MissionMultiEtapesScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(missionMultiEtapesProvider.notifier).chargerTourneeDemo());
  }

  @override
  Widget build(BuildContext context) {
    final tournee = ref.watch(missionMultiEtapesProvider).tournee;

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(tournee == null ? 'Tournée groupée' : tournee.axeLibelle)),
      body: tournee == null
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
                          'Démonstration — tournée simulée en attendant le multi-étapes côté service-opt.',
                          style: TextStyle(color: AppColors.accent, fontSize: 12),
                        ),
                      ),
                    ]),
                  ),
                  const SizedBox(height: 20),
                  Text('Envoi groupé — ${tournee.etapes.length} étapes', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 16),
                  if (tournee.terminee) _carteTourneeTerminee() else _carteEtapeCourante(tournee.etapeCourante!),
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
    );
  }

  Widget _carteEtapeCourante(EtapeTournee etape) {
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
                  Text(etape.lieuNom, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                  if (etape.envoiReference != null)
                    Text('Réf. ${etape.envoiReference}', style: const TextStyle(color: AppColors.texteMuet, fontSize: 11)),
                ],
              ),
            ),
          ]),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton(
              onPressed: () => ref.read(missionMultiEtapesProvider.notifier).confirmerEtapeCourante(),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: Text(estEnlevement ? 'Confirmer l\'enlèvement' : 'Confirmer la livraison',
                  style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
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
              Text(etape.lieuNom, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
            ],
          ),
        ),
      ]),
    );
  }
}
