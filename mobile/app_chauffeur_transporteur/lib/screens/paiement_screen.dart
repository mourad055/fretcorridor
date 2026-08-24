import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/paiement_provider.dart';
import '../theme/app_theme.dart';

Map<String, String> _libellesNature(AppLocalizations t) => {
      'ENCAISSEMENT': t.natureEncaissement,
      'REVERSEMENT': t.natureReversement,
      'COMMISSION': t.natureCommission,
      'SEQUESTRE': t.natureSequestre,
    };

// S14 (EF-PAY-06/07) : les 4 valeurs réelles de ModePaiement côté
// service-pay — plus fin que ça (MoMo vs Orange Money) n'est pas distingué
// par le backend, qui ne connaît que "monnaie électronique" en général.
Map<String, String> _libellesModePaiement(AppLocalizations t) => {
      'MONNAIE_ELECTRONIQUE': t.modeMonnaieElectronique,
      'VIREMENT': t.modeVirement,
      'TERME_CONTRACTUEL': t.modeTermeContractuel,
      'ESPECES': t.modeEspeces,
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
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.soldeEtGains)),
      body: RefreshIndicator(
        onRefresh: () => ref.read(paiementProvider.notifier).chargerSolde(),
        child: state.chargement && state.historique.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null
                ? _erreur(state.erreur!)
                : ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      _carteSolde(t, state.solde),
                      const SizedBox(height: 24),
                      Text(t.historique, style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 12),
                      if (state.historique.isEmpty)
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 24),
                          child: Center(
                            child: Text(t.aucuneEcriturePourLeMoment, style: const TextStyle(color: AppColors.texteMuet)),
                          ),
                        )
                      else
                        ...state.historique.map((e) => _carteEcriture(t, e, state.modePaiementParMission)),
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

  Widget _carteSolde(AppLocalizations t, double solde) {
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
          Text(t.soldeLabel, style: const TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          Text('${solde.toStringAsFixed(0)} XAF',
              style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold, color: AppColors.texte)),
        ],
      ),
    );
  }

  Widget _carteEcriture(AppLocalizations t, Ecriture e, Map<String, String> modePaiementParMission) {
    final positif = e.sens == 'CREDIT';
    final modePaiement = e.nature == 'ENCAISSEMENT' ? modePaiementParMission[e.missionId] : null;
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
              Text(_libellesNature(t)[e.nature] ?? e.nature, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              Text(e.statut, style: const TextStyle(color: AppColors.texteMuet, fontSize: 11)),
              if (modePaiement != null) ...[
                const SizedBox(height: 2),
                Row(children: [
                  const Icon(Icons.payments_outlined, color: AppColors.texteMuet, size: 12),
                  const SizedBox(width: 4),
                  Text(t.regleVia(_libellesModePaiement(t)[modePaiement] ?? modePaiement),
                      style: const TextStyle(color: AppColors.texteMuet, fontSize: 11)),
                ]),
              ],
            ],
          ),
        ),
        Text('${positif ? '+' : '-'}${e.montant.toStringAsFixed(0)} XAF',
            style: TextStyle(fontWeight: FontWeight.bold, color: positif ? AppColors.succes : AppColors.texte)),
      ]),
    );
  }
}
