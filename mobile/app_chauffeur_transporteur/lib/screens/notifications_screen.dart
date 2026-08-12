import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/notification_provider.dart';
import '../theme/app_theme.dart';

const _iconesType = {
  'MISSION': Icons.local_shipping_outlined,
  'PAIEMENT': Icons.account_balance_wallet_outlined,
  'KYC': Icons.badge_outlined,
};

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
    Future.microtask(() => ref.read(notificationProvider.notifier).charger());
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(notificationProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Notifications')),
      body: RefreshIndicator(
        onRefresh: () => ref.read(notificationProvider.notifier).charger(),
        child: state.chargement && state.notifications.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null
                ? _erreur(state.erreur!)
                : state.notifications.isEmpty
                    ? ListView(children: const [
                        SizedBox(height: 80),
                        Center(child: Text('Aucune notification.', style: TextStyle(color: AppColors.texteMuet))),
                      ])
                    : ListView.separated(
                        padding: const EdgeInsets.all(16),
                        itemCount: state.notifications.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 8),
                        itemBuilder: (context, i) => _carteNotification(state.notifications[i]),
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

  Widget _carteNotification(AppNotification n) {
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
                Text(n.titre, style: TextStyle(fontWeight: n.lue ? FontWeight.normal : FontWeight.bold, fontSize: 14)),
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
