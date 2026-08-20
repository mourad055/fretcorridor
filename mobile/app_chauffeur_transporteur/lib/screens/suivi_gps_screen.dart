import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/position_provider.dart';
import '../theme/app_theme.dart';

// S6 (EF-TRK-01) : suivi GPS arrière-plan pendant une mission. La saisie
// manuelle de l'identifiant de mission est temporaire — l'écran "mission en
// cours" (S7, pas encore construit côté service-exe : aucun lien
// mission↔chauffeur n'existe pour l'instant) appellera positionProvider
// automatiquement une fois disponible.
class SuiviGpsScreen extends ConsumerStatefulWidget {
  const SuiviGpsScreen({super.key});

  @override
  ConsumerState<SuiviGpsScreen> createState() => _SuiviGpsScreenState();
}

class _SuiviGpsScreenState extends ConsumerState<SuiviGpsScreen> {
  final _missionIdCtrl = TextEditingController();

  @override
  void dispose() {
    _missionIdCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(positionProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Suivi GPS')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.surface,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: AppColors.bordure),
                ),
                child: const Row(children: [
                  Icon(Icons.info_outline, color: AppColors.accentProfond, size: 18),
                  SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Identifiant de mission à saisir manuellement pour l\'instant — sera automatique '
                      'une fois l\'écran "mission en cours" disponible.',
                      style: TextStyle(color: AppColors.texteMuet, fontSize: 12),
                    ),
                  ),
                ]),
              ),
              const SizedBox(height: 20),

              const Text('IDENTIFIANT DE MISSION',
                  style: TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              TextField(
                controller: _missionIdCtrl,
                enabled: !state.suiviActif,
                decoration: InputDecoration(
                  filled: true,
                  fillColor: AppColors.surface,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
                ),
              ),
              const SizedBox(height: 20),

              if (state.suiviActif)
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppColors.succes.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: AppColors.succes.withValues(alpha: 0.4)),
                  ),
                  child: Row(children: [
                    const Icon(Icons.gps_fixed, color: AppColors.succes),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        state.dernierEnvoi == null
                            ? 'Suivi actif — premier envoi en cours…'
                            : 'Suivi actif — dernier envoi à '
                                '${state.dernierEnvoi!.hour.toString().padLeft(2, '0')}:${state.dernierEnvoi!.minute.toString().padLeft(2, '0')}:${state.dernierEnvoi!.second.toString().padLeft(2, '0')}',
                        style: const TextStyle(color: AppColors.succes, fontSize: 13),
                      ),
                    ),
                  ]),
                ),
              if (state.erreur != null) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppColors.erreur.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
                  ),
                  child: Row(children: [
                    const Icon(Icons.warning_amber, color: AppColors.erreur),
                    const SizedBox(width: 10),
                    Expanded(child: Text(state.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 13))),
                  ]),
                ),
              ],
              const SizedBox(height: 20),

              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton.icon(
                  onPressed: () {
                    if (state.suiviActif) {
                      ref.read(positionProvider.notifier).arreterSuivi();
                    } else {
                      if (_missionIdCtrl.text.trim().isEmpty) return;
                      ref.read(positionProvider.notifier).demarrerSuivi(_missionIdCtrl.text.trim());
                    }
                  },
                  icon: Icon(state.suiviActif ? Icons.stop : Icons.play_arrow, color: AppColors.texteBouton),
                  label: Text(state.suiviActif ? 'Arrêter le suivi' : 'Démarrer le suivi',
                      style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: state.suiviActif ? AppColors.erreur : AppColors.accent,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
