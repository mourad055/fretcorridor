import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import 'agent_enrolement_screen.dart';
import 'axes_screen.dart';
import 'capacite_screen.dart';
import 'kyc_screen.dart';
import 'login_screen.dart';
import 'missions_screen.dart';
import 'notifications_screen.dart';
import 'paiement_screen.dart';
import 'suivi_gps_screen.dart';
import 'vehicules_screen.dart';
import '../providers/notification_provider.dart';

// Preuve que le login fonctionne de bout en bout (gateway -> service-ida) :
// affiche le rôle et le tenant réellement résolus depuis le JWT. Les écrans
// métier (KYC, capacité, missions...) viennent aux sprints suivants.
class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(notificationProvider.notifier).charger());
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final nombreNonLues = ref.watch(notificationProvider).nombreNonLues;

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(
        title: const Text('FretCorridor'),
        actions: [
          IconButton(
            icon: Badge(
              label: Text('$nombreNonLues'),
              isLabelVisible: nombreNonLues > 0,
              child: const Icon(Icons.notifications_outlined, color: AppColors.texteMuet),
            ),
            tooltip: 'Notifications',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const NotificationsScreen()),
              );
            },
          ),
          if (const ['CHAUFFEUR', 'TRANSPORTEUR', 'CHAUFFEUR_PROPRIETAIRE'].contains(authState.role)) ...[
            IconButton(
              icon: const Icon(Icons.assignment_outlined, color: AppColors.texteMuet),
              tooltip: 'Mes missions',
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const MissionsScreen()),
                );
              },
            ),
            IconButton(
              icon: const Icon(Icons.local_shipping_outlined, color: AppColors.texteMuet),
              tooltip: 'Déclarer une capacité',
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const CapaciteScreen()),
                );
              },
            ),
            IconButton(
              icon: const Icon(Icons.garage_outlined, color: AppColors.texteMuet),
              tooltip: 'Ma flotte',
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const VehiculesScreen()),
                );
              },
            ),
            IconButton(
              icon: const Icon(Icons.gps_fixed, color: AppColors.texteMuet),
              tooltip: 'Suivi GPS',
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const SuiviGpsScreen()),
                );
              },
            ),
            IconButton(
              icon: const Icon(Icons.account_balance_wallet_outlined, color: AppColors.texteMuet),
              tooltip: 'Solde et gains',
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const PaiementScreen()),
                );
              },
            ),
          ],
          IconButton(
            icon: const Icon(Icons.route_outlined, color: AppColors.texteMuet),
            tooltip: 'Axes',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const AxesScreen()),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.badge_outlined, color: AppColors.texteMuet),
            tooltip: 'Mon profil',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const KycScreen()),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.logout, color: AppColors.texteMuet),
            onPressed: () async {
              await ref.read(authProvider.notifier).logout();
              if (context.mounted) {
                Navigator.pushAndRemoveUntil(
                  context,
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                  (route) => false,
                );
              }
            },
          ),
        ],
      ),
      floatingActionButton: authState.role == 'AGENT'
          ? FloatingActionButton.extended(
              onPressed: () {
                Navigator.push(context, MaterialPageRoute(builder: (_) => const AgentEnrolementScreen()));
              },
              backgroundColor: AppColors.accent,
              icon: const Icon(Icons.person_add_alt_1, color: AppColors.texteBouton),
              label: const Text('Enrôler', style: TextStyle(color: AppColors.texteBouton, fontWeight: FontWeight.bold)),
            )
          : null,
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppColors.bordure),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(children: [
                  Icon(Icons.check_circle, color: AppColors.succes),
                  SizedBox(width: 8),
                  Text('Connecté', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ]),
                const SizedBox(height: 12),
                Text('Rôle : ${authState.role ?? '—'}',
                    style: const TextStyle(color: AppColors.texteMuet)),
                Text('Tenant : ${authState.tenantId ?? '—'}',
                    style: const TextStyle(color: AppColors.texteMuet)),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
