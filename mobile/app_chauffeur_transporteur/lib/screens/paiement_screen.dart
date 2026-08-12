import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/paiement_provider.dart';
import '../theme/app_theme.dart';

const _libellesNature = {
  'ENCAISSEMENT': 'Encaissement',
  'REVERSEMENT': 'Reversement',
  'COMMISSION': 'Commission',
  'SEQUESTRE': 'Séquestre',
};

// S8 : solde et historique des gains — lecture seule (ENF-FIN-01, aucune
// écriture depuis le mobile), consomme le grand livre miroir de service-pay.
class PaiementScreen extends ConsumerStatefulWidget {
  const PaiementScreen({super.key});

  @override
  ConsumerState<PaiementScreen> createState() => _PaiementScreenState();
}

class _PaiementScreenState extends ConsumerState<PaiementScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(paiementProvider.notifier).chargerSolde());
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(paiementProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Solde et gains')),
      body: RefreshIndicator(
        onRefresh: () => ref.read(paiementProvider.notifier).chargerSolde(),
        child: state.chargement && state.historique.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null
                ? _erreur(state.erreur!)
                : ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      _carteSolde(state.solde),
                      const SizedBox(height: 24),
                      Text('Historique', style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 12),
                      if (state.historique.isEmpty)
                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 24),
                          child: Center(
                            child: Text('Aucune écriture pour le moment.', style: TextStyle(color: AppColors.texteMuet)),
                          ),
                        )
                      else
                        ...state.historique.map(_carteEcriture),
                    ],
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

  Widget _carteSolde(double solde) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.accentProfond.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.accentProfond.withValues(alpha: 0.35)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('SOLDE', style: TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          Text('${solde.toStringAsFixed(0)} XAF',
              style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold, color: AppColors.texte)),
        ],
      ),
    );
  }

  Widget _carteEcriture(Ecriture e) {
    final positif = e.sens == 'CREDIT';
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Row(children: [
        Icon(positif ? Icons.arrow_downward : Icons.arrow_upward, color: positif ? AppColors.succes : AppColors.texteMuet, size: 18),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(_libellesNature[e.nature] ?? e.nature, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              Text(e.statut, style: const TextStyle(color: AppColors.texteMuet, fontSize: 11)),
            ],
          ),
        ),
        Text('${positif ? '+' : '-'}${e.montant.toStringAsFixed(0)} XAF',
            style: TextStyle(fontWeight: FontWeight.bold, color: positif ? AppColors.succes : AppColors.texte)),
      ]),
    );
  }
}
