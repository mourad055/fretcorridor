import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/notification_provider.dart';
import '../providers/proposition_retour_provider.dart';
import '../theme/app_theme.dart';

const _iconesType = {
  'MISSION': Icons.local_shipping_outlined,
  'PAIEMENT': Icons.account_balance_wallet_outlined,
  'KYC': Icons.badge_outlined,
};

// BUG CORRIGE (retour utilisatrice 24/08) : titre/corps viennent du serveur
// en français en dur (NotificationService.creer, service-not), jamais
// traduits meme en changeant la langue de l'app. Correctif cote client
// uniquement, portee volontairement limitee : le TITRE est retraduit ici a
// partir de `type` (5 valeurs d'enum connues, deja transmises), le CORPS
// reste en francais -- meme principe deja documente pour le texte
// dynamique du serveur ailleurs dans l'app (motifClassement, etc.) : le
// traduire sans toucher a ce qui est reellement persiste cote serveur
// creerait un decalage. Une vraie traduction complete demanderait de
// remanier NotificationService pour envoyer des parametres structures
// plutot que des phrases toutes faites -- hors portee d'un correctif la
// veille d'une presentation.
String _titreLocalise(AppLocalizations t, AppNotification n) {
  switch (n.type) {
    case 'PROPOSITION_RECUE':
      return t.notifTitrePropositionRecue;
    case 'STATUT_MISSION':
      return t.notifTitreStatutMission;
    case 'INFO_GENERALE':
      return t.notifTitreInfoGenerale;
    case 'PROPOSITION_RETOUR':
      return t.notifTitrePropositionRetour;
    case 'ALERTE_ECART':
      return t.notifTitreAlerteEcart;
    default:
      return n.titre;
  }
}

// S9 : centre de notifications (réception "tirée" — pas de push FCM réel,
// aucun projet Firebase disponible pour ce dépôt, cf. README).
class NotificationsScreen extends ConsumerStatefulWidget {
  const NotificationsScreen({super.key});

  @override
  ConsumerState<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends ConsumerState<NotificationsScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(notificationProvider.notifier).charger();
      ref.read(propositionRetourProvider.notifier).charger();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(notificationProvider);
    final propositionsEnAttente = ref.watch(propositionRetourProvider).enAttente;
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.notifications)),
      body: RefreshIndicator(
        onRefresh: () => Future.wait([
          ref.read(notificationProvider.notifier).charger(),
          ref.read(propositionRetourProvider.notifier).charger(),
        ]),
        child: state.chargement && state.notifications.isEmpty && propositionsEnAttente.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null
                ? _erreur(state.erreur!)
                : state.notifications.isEmpty && propositionsEnAttente.isEmpty
                    ? ListView(children: [
                        const SizedBox(height: 80),
                        Center(child: Text(t.aucuneNotification, style: const TextStyle(color: AppColors.texteMuet))),
                      ])
                    : ListView(
                        padding: const EdgeInsets.all(16),
                        children: [
                          for (final p in propositionsEnAttente) ...[
                            _cartePropositionRetour(t, p),
                            const SizedBox(height: 8),
                          ],
                          for (final n in state.notifications) ...[
                            _carteNotification(t, n),
                            const SizedBox(height: 8),
                          ],
                        ],
                      ),
      ),
    );
  }

  // S12 — proposition de mission retour après une livraison, avec
  // acceptation/refus par le chauffeur (voir proposition_retour_provider.dart).
  Widget _cartePropositionRetour(AppLocalizations t, PropositionRetour p) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surfaceClaire,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.accent.withValues(alpha: 0.4)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            const Icon(Icons.sync_alt, color: AppColors.accent),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(p.titre, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  const SizedBox(height: 2),
                  Text(p.corps, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                ],
              ),
            ),
          ]),
          const SizedBox(height: 12),
          Row(children: [
            Expanded(
              child: OutlinedButton(
                onPressed: () => ref.read(propositionRetourProvider.notifier).refuser(p.id),
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: AppColors.erreur),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
                child: Text(t.refuser, style: const TextStyle(color: AppColors.erreur)),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: ElevatedButton(
                onPressed: () => ref.read(propositionRetourProvider.notifier).accepter(p.id),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.accent,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
                child: Text(t.accepter, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
              ),
            ),
          ]),
        ],
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

  Widget _carteNotification(AppLocalizations t, AppNotification n) {
    return InkWell(
      onTap: n.lue ? null : () => ref.read(notificationProvider.notifier).marquerLue(n.id),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: n.lue ? AppColors.surface : AppColors.surfaceClaire,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: n.lue ? AppColors.bordure : AppColors.accent.withValues(alpha: 0.3)),
        ),
        child: Row(children: [
          Icon(_iconesType[n.type] ?? Icons.notifications_outlined, color: n.lue ? AppColors.texteMuet : AppColors.accent),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(_titreLocalise(t, n), style: TextStyle(fontWeight: n.lue ? FontWeight.normal : FontWeight.bold, fontSize: 14)),
                const SizedBox(height: 4),
                Text(n.corps, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
              ],
            ),
          ),
          if (!n.lue) Container(width: 8, height: 8, decoration: const BoxDecoration(color: AppColors.accent, shape: BoxShape.circle)),
        ]),
      ),
    );
  }
}
