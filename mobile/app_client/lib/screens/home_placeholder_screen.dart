import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';
import '../providers/kyc_provider.dart';
import '../providers/notification_provider.dart';
import '../theme/app_theme.dart';
import 'completer_profil_screen.dart';
import 'menu_drawer.dart';
import 'mes_demandes_screen.dart';
import 'notifications_screen.dart';
import 'promo_carousel.dart';

class HomePlaceholderScreen extends ConsumerWidget {
  const HomePlaceholderScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final kycState = ref.watch(kycProvider);
    final niveauValide = kycState.niveauKyc != 'NIVEAU_0';
    final notifState = ref.watch(notificationProvider);
    final authState = ref.watch(authProvider);

    final nomAffiche = kycState.type == 'ENTREPRISE' && (kycState.raisonSociale?.isNotEmpty ?? false)
        ? kycState.raisonSociale!
        : (kycState.prenom?.isNotEmpty ?? false)
            ? kycState.prenom!
            : 'Chargeur';
    final initiale = nomAffiche.isNotEmpty ? nomAffiche[0].toUpperCase() : '?';

    // Passage à un profil complet : notification brève plutôt qu'un bandeau
    // "Profil complet" affiché en permanence sur l'accueil.
    ref.listen(kycProvider, (previous, next) {
      final etaitComplet = previous != null && previous.niveauKyc != 'NIVEAU_0';
      final estComplet = next.niveauKyc != 'NIVEAU_0';
      if (!etaitComplet && estComplet) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Row(children: [
              Icon(Icons.check_circle, color: Colors.white, size: 20),
              SizedBox(width: 10),
              Text('Profil complété ✅ — vous pouvez publier une demande.'),
            ]),
            backgroundColor: AppColors.succes,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    });

    return Scaffold(
      backgroundColor: AppColors.fond,
      drawer: const MenuDrawer(),
      body: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.fromLTRB(16, 0, 20, 32),
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [AppColors.accent, AppColors.accentProfond],
                ),
                borderRadius: BorderRadius.vertical(bottom: Radius.circular(28)),
              ),
              child: SafeArea(
                bottom: false,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Builder(builder: (context) => IconButton(
                              icon: const Icon(Icons.menu, color: Colors.white),
                              onPressed: () => Scaffold.of(context).openDrawer(),
                            )),
                        Row(children: [
                          IconButton(
                            icon: Badge(
                              label: Text('${notifState.nombreNonLues}'),
                              isLabelVisible: notifState.nombreNonLues > 0,
                              child: const Icon(Icons.notifications_outlined, color: Colors.white),
                            ),
                            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const NotificationsScreen())),
                          ),
                          GestureDetector(
                            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CompleterProfilScreen())),
                            child: CircleAvatar(
                              radius: 18,
                              backgroundColor: Colors.white.withValues(alpha: 0.25),
                              child: Text(initiale, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                            ),
                          ),
                        ]),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Bonjour, $nomAffiche',
                              style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: Colors.white)),
                          const SizedBox(height: 2),
                          Text(authState.tenantId ?? 'Marketplace CEMAC', style: const TextStyle(color: Colors.white70, fontSize: 13)),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.all(20),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                const PromoCarousel(),
                const SizedBox(height: 24),

                // ── Statut KYC — seulement si à compléter (sinon notification, cf. ref.listen ci-dessus) ──
                if (!niveauValide) ...[
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceClaire,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppColors.accent, width: 1),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.info_outline, color: AppColors.accent),
                        const SizedBox(width: 12),
                        const Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('Profil à compléter', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                              Text('Complétez votre profil pour publier une demande.',
                                  style: TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                            ],
                          ),
                        ),
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
                ],

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
              ]),
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
