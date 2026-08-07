import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/demande_provider.dart';
import '../models/demande_model.dart';
import '../theme/app_theme.dart';
import 'publier_demande_screen.dart';
import 'propositions_screen.dart';
import 'suivi_screen.dart';

class MesDemandesScreen extends ConsumerWidget {
  const MesDemandesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final demandeState = ref.watch(demandeProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(
        title: const Text('Mes demandes'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: AppColors.texteMuet),
            onPressed: () => ref.read(demandeProvider.notifier).chargerMesDemandes(),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppColors.accent,
        onPressed: () async {
          await Navigator.push(context, MaterialPageRoute(builder: (_) => const PublierDemandeScreen()));
          ref.read(demandeProvider.notifier).chargerMesDemandes();
        },
        icon: const Icon(Icons.add, color: Colors.white),
        label: const Text('Nouvelle demande', style: TextStyle(color: Colors.white)),
      ),
      body: demandeState.chargement
          ? const Center(child: CircularProgressIndicator(color: AppColors.accent))
          : demandeState.mesDemandes.isEmpty
              ? const Center(
                  child: Padding(
                    padding: EdgeInsets.all(24),
                    child: Text('Aucune demande publiée pour le moment.',
                        style: TextStyle(color: AppColors.texteMuet), textAlign: TextAlign.center),
                  ),
                )
              : RefreshIndicator(
                  color: AppColors.accent,
                  onRefresh: () => ref.read(demandeProvider.notifier).chargerMesDemandes(),
                  child: ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: demandeState.mesDemandes.length,
                    itemBuilder: (context, i) => _DemandeCard(demande: demandeState.mesDemandes[i]),
                  ),
                ),
    );
  }
}

class _DemandeCard extends StatelessWidget {
  final DemandeModel demande;
  const _DemandeCard({required this.demande});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () => Navigator.push(
        context,
        MaterialPageRoute(builder: (_) => PropositionsScreen(demande: demande)),
      ),
      borderRadius: BorderRadius.circular(12),
      child: Container(
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
            Row(children: [
              const Icon(Icons.trip_origin, size: 14, color: AppColors.texteMuet),
              Text(' ${demande.villeDepart} ', style: const TextStyle(fontSize: 13)),
              const Icon(Icons.arrow_forward, size: 12, color: AppColors.texteMuet),
              const Icon(Icons.place, size: 14, color: AppColors.texteMuet),
              Text(' ${demande.villeArrivee}', style: const TextStyle(fontSize: 13)),
            ]),
            const SizedBox(height: 6),
            Text('${demande.typeEmballageNom} × ${demande.quantite} — ${demande.poidsTotalKg.toStringAsFixed(0)} kg',
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
            const SizedBox(height: 6),
            Row(children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: AppColors.surfaceClaire,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(demande.statut, style: const TextStyle(fontSize: 10, color: AppColors.accent, fontWeight: FontWeight.bold)),
              ),
              const Spacer(),
              TextButton.icon(
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => SuiviScreen(demandeId: demande.id)),
                ),
                icon: const Icon(Icons.location_on_outlined, size: 14, color: AppColors.accent),
                label: const Text('Suivi', style: TextStyle(fontSize: 11, color: AppColors.accent)),
                style: TextButton.styleFrom(padding: EdgeInsets.zero, minimumSize: const Size(0, 0)),
              ),
              const SizedBox(width: 6),
              const Text('Voir les propositions', style: TextStyle(fontSize: 11, color: AppColors.texteMuet)),
              const Icon(Icons.chevron_right, size: 16, color: AppColors.texteMuet),
            ]),
          ],
        ),
      ),
    );
  }
}
