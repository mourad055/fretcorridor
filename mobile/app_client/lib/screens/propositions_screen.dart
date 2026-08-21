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
                  ..._propositions.map((p) => _CarteProposition(proposition: p)),
              ],
            ),
    );
  }
}

class _CarteProposition extends StatelessWidget {
  final Map<String, dynamic> proposition;
  const _CarteProposition({required this.proposition});

  Color _couleurStatut(String statut) {
    switch (statut) {
      case 'ACCEPTEE':
        return AppColors.succes;
      case 'EXPIREE':
        return AppColors.erreur;
      default:
        return AppColors.accent;
    }
  }

  String _libelleStatut(String statut) {
    switch (statut) {
      case 'ACCEPTEE':
        return 'Acceptée';
      case 'EXPIREE':
        return 'Expirée';
      default:
        return 'En attente';
    }
  }

  @override
  Widget build(BuildContext context) {
    final rang = proposition['rang']?.toString() ?? '?';
    final motif = proposition['motifClassement'] as String?;
    final prix = proposition['prixEstime'] as String?;
    final statut = proposition['statut'] as String? ?? 'EN_ATTENTE';
    final couleur = _couleurStatut(statut);

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 40, height: 40,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppColors.surfaceClaire,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Text('#$rang', style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.accent)),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      prix != null ? '$prix XAF' : 'Prix en cours de calcul',
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: couleur.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(_libelleStatut(statut),
                          style: TextStyle(color: couleur, fontSize: 11, fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
                if (motif != null && motif.isNotEmpty) ...[
                  const SizedBox(height: 6),
                  Text(motif, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
