import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/notification_provider.dart';
import '../theme/app_theme.dart';

class NotificationsScreen extends ConsumerStatefulWidget {
  const NotificationsScreen({super.key});

  @override
  ConsumerState<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends ConsumerState<NotificationsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => ref.read(notificationProvider.notifier).charger());
  }

  IconData _iconePourType(String type) {
    switch (type) {
      case 'PROPOSITION_RECUE': return Icons.local_offer;
      case 'STATUT_MISSION': return Icons.local_shipping;
      default: return Icons.info_outline;
    }
  }

  // BUG CORRIGE (retour utilisatrice 24/08) : voir le commentaire equivalent
  // dans app_chauffeur_transporteur/lib/screens/notifications_screen.dart --
  // meme correctif, meme portee (titre retraduit via `type`, corps laisse en
  // francais).
  String _titreLocalise(AppLocalizations t, String type, String titreServeur) {
    switch (type) {
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
        return titreServeur;
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(notificationProvider);
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.notifications)),
      body: state.chargement
          ? const Center(child: CircularProgressIndicator(color: AppColors.accent))
          : state.notifications.isEmpty
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Text(t.aucuneNotification,
                        style: const TextStyle(color: AppColors.texteMuet), textAlign: TextAlign.center),
                  ),
                )
              : RefreshIndicator(
                  color: AppColors.accent,
                  onRefresh: () => ref.read(notificationProvider.notifier).charger(),
                  child: ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: state.notifications.length,
                    itemBuilder: (context, i) {
                      final n = state.notifications[i];
                      return InkWell(
                        onTap: () {
                          if (!n.lue) ref.read(notificationProvider.notifier).marquerCommeLue(n.id);
                        },
                        borderRadius: BorderRadius.circular(12),
                        child: Container(
                          margin: const EdgeInsets.only(bottom: 8),
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: n.lue ? AppColors.surface : AppColors.surfaceClaire,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: AppColors.bordure),
                          ),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Icon(_iconePourType(n.type), color: AppColors.accent, size: 20),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(_titreLocalise(t, n.type, n.titre), style: TextStyle(
                                        fontWeight: n.lue ? FontWeight.normal : FontWeight.bold, fontSize: 13)),
                                    const SizedBox(height: 2),
                                    Text(n.corps, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                                  ],
                                ),
                              ),
                              if (!n.lue)
                                Container(
                                  width: 8, height: 8,
                                  decoration: const BoxDecoration(color: AppColors.accent, shape: BoxShape.circle),
                                ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
    );
  }
}
