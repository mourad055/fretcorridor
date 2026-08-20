import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';
import '../providers/kyc_provider.dart';
import '../providers/notification_provider.dart';
import '../theme/app_theme.dart';
import 'login_screen.dart';
import 'completer_profil_screen.dart';
import 'mes_demandes_screen.dart';
import 'notifications_screen.dart';

class HomePlaceholderScreen extends ConsumerWidget {
  const HomePlaceholderScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final kycState = ref.watch(kycProvider);
    final niveauValide = kycState.niveauKyc != 'NIVEAU_0';
    final notifState = ref.watch(notificationProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(
        title: Row(
          children: [
            Image.asset('assets/images/logo_fretcorridor.jpeg', height: 28),
            const SizedBox(width: 8),
            const Text('FretCorridor', style: TextStyle(fontSize: 16)),
          ],
        ),
        actions: [
          Stack(
            alignment: Alignment.center,
            children: [
              IconButton(
                icon: const Icon(Icons.notifications_outlined, color: AppColors.texteMuet),
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const NotificationsScreen()),
                ),
              ),
              if (notifState.nombreNonLues > 0)
                Positioned(
                  top: 10, right: 10,
                  child: Container(
                    padding: const EdgeInsets.all(3),
                    decoration: const BoxDecoration(color: AppColors.accent, shape: BoxShape.circle),
                    constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
                    child: Text(
                      '${notifState.nombreNonLues}',
                      style: const TextStyle(color: Colors.white, fontSize: 9, fontWeight: FontWeight.bold),
                      textAlign: TextAlign.center,
                    ),
                  ),
                ),
            ],
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
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          // ── Statut KYC ──────────────────────────────────
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: niveauValide ? const Color(0xFFE7F5EC) : AppColors.surfaceClaire,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: niveauValide ? const Color(0xFF15803D) : AppColors.accent, width: 1),
            ),
            child: Row(
              children: [
                Icon(
                  niveauValide ? Icons.check_circle : Icons.info_outline,
                  color: niveauValide ? const Color(0xFF15803D) : AppColors.accent,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        niveauValide ? 'Profil complet — ${kycState.niveauKyc}' : 'Profil à compléter',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                      ),
                      Text(
                        niveauValide
                            ? 'Vous pouvez publier des demandes de transport.'
                            : 'Complétez votre profil pour publier une demande.',
                        style: const TextStyle(color: AppColors.texteMuet, fontSize: 12),
                      ),
                    ],
                  ),
                ),
                if (!niveauValide)
                  TextButton(
                    onPressed: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const CompleterProfilScreen()),
                    ),
                    child: const Text('Compléter'),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 24),

          _CarteAction(
            icone: Icons.local_shipping_outlined,
            titre: 'Envoyer une marchandise',
            description: 'Publiez une demande via le catalogue d\'emballages',
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const MesDemandesScreen()),
            ),
          ),
          const SizedBox(height: 12),
          _CarteAction(
            icone: Icons.person_outline,
            titre: 'Mon profil',
            description: 'Informations personnelles et niveau KYC',
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const CompleterProfilScreen()),
            ),
          ),
        ],
      ),
    );
  }
}

class _CarteAction extends StatelessWidget {
  final IconData icone;
  final String titre;
  final String description;
  final VoidCallback onTap;

  const _CarteAction({
    required this.icone, required this.titre, required this.description, required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.bordure),
        ),
        child: Row(
          children: [
            Container(
              width: 44, height: 44,
              decoration: BoxDecoration(color: AppColors.surfaceClaire, borderRadius: BorderRadius.circular(10)),
              child: Icon(icone, color: AppColors.accent),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(titre, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  Text(description, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: AppColors.texteMuet),
          ],
        ),
      ),
    );
  }
}
