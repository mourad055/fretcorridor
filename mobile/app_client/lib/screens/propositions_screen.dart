import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/demande_provider.dart';
import '../models/demande_model.dart';
import '../theme/app_theme.dart';

// UC-MKT-02 : au plus 3 propositions, ordonnées, avec motif de classement.
// Tant que service-mat/service-opt (Moteur) ne sont pas branchés, l'API
// renvoie une liste vide — l'écran gère déjà cet état proprement.
class PropositionsScreen extends ConsumerStatefulWidget {
  final DemandeModel demande;
  const PropositionsScreen({super.key, required this.demande});

  @override
  ConsumerState<PropositionsScreen> createState() => _PropositionsScreenState();
}

class _PropositionsScreenState extends ConsumerState<PropositionsScreen> {
  List<Map<String, dynamic>> _propositions = [];
  bool _chargement = true;

  @override
  void initState() {
    super.initState();
    _charger();
  }

  Future<void> _charger() async {
    final props = await ref.read(demandeProvider.notifier).getPropositions(widget.demande.id);
    if (mounted) setState(() { _propositions = props; _chargement = false; });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Propositions')),
      body: _chargement
          ? const Center(child: CircularProgressIndicator(color: AppColors.accent))
          : ListView(
              padding: const EdgeInsets.all(20),
              children: [
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppColors.surface,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: AppColors.bordure),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${widget.demande.villeDepart} → ${widget.demande.villeArrivee}',
                          style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 4),
                      Text('${widget.demande.typeEmballageNom} × ${widget.demande.quantite}',
                          style: const TextStyle(color: AppColors.texteMuet, fontSize: 13)),
                    ],
                  ),
                ),
                const SizedBox(height: 24),

                if (_propositions.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 40),
                    child: Column(
                      children: [
                        const Icon(Icons.hourglass_empty, color: AppColors.bordure, size: 48),
                        const SizedBox(height: 12),
                        const Text('Aucune proposition pour le moment',
                            style: TextStyle(color: AppColors.texteMuet, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 6),
                        const Text(
                          'Votre demande est en attente d\'appariement avec un transporteur disponible sur cet axe.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: AppColors.texteMuet, fontSize: 12),
                        ),
                      ],
                    ),
                  )
                else
                  ..._propositions.map((p) => Container(
                        margin: const EdgeInsets.only(bottom: 10),
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: AppColors.surface,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: AppColors.bordure),
                        ),
                        child: Text(p.toString()),
                      )),
              ],
            ),
    );
  }
}
