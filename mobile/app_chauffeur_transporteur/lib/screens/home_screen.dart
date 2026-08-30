import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/auth_provider.dart';
import '../providers/kyc_provider.dart';
import '../theme/app_theme.dart';
import 'agent_enrolement_screen.dart';
import 'axes_screen.dart';
import 'capacite_screen.dart';
import 'kyc_screen.dart';
import 'menu_drawer.dart';
import 'missions_screen.dart';
import 'notifications_screen.dart';
import 'paiement_screen.dart';
import 'promo_carousel.dart';
import 'propositions_mission_screen.dart';
import 'vehicules_screen.dart';
import '../providers/notification_provider.dart';
import '../widgets/top_notification.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(notificationProvider.notifier).charger();
      ref.read(kycProvider.notifier).chargerProfil();
    });
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final profil = ref.watch(kycProvider).profil;
    final nombreNonLues = ref.watch(notificationProvider).nombreNonLues;
    final estTransporteur = const ['CHAUFFEUR', 'TRANSPORTEUR', 'CHAUFFEUR_PROPRIETAIRE'].contains(authState.role);
    final t = AppLocalizations.of(context);

    final nomAffiche = _nomAffiche(t, profil, authState.role);
    final initiale = nomAffiche.isNotEmpty ? nomAffiche[0].toUpperCase() : '?';
    final niveauValide = profil != null && profil.niveauKyc != 'NIVEAU_0';

    // Passage à un profil complet : notification brève plutôt qu'un bandeau
    // affiché en permanence sur l'accueil (même patron que l'app Client).
    ref.listen(kycProvider, (previous, next) {
      final etaitComplet = previous?.profil != null && previous!.profil!.niveauKyc != 'NIVEAU_0';
      final estComplet = next.profil != null && next.profil!.niveauKyc != 'NIVEAU_0';
      if (!etaitComplet && estComplet) {
        afficherNotification(
          context,
          message: t.profilCompleteMessageChauffeur,
          couleur: AppColors.succes,
          icone: Icons.check_circle,
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
                              label: Text('$nombreNonLues'),
                              isLabelVisible: nombreNonLues > 0,
                              child: const Icon(Icons.notifications_outlined, color: Colors.white),
                            ),
                            tooltip: t.notificationsTooltip,
                            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const NotificationsScreen())),
                          ),
                          GestureDetector(
                            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const KycScreen())),
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
                          Text(t.bonjour(nomAffiche),
                              style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: Colors.white)),
                          const SizedBox(height: 2),
                          Text(authState.tenantId ?? '—', style: const TextStyle(color: Colors.white70, fontSize: 13)),
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
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(t.profilACompleter, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                              Text(t.profilACompleterDescriptionChauffeur,
                                  style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                            ],
                          ),
                        ),
                        TextButton(
                          onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const KycScreen())),
                          child: Text(t.completer),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                ],

                if (estTransporteur) ...[
                  _CarteAction(
                    icone: Icons.notifications_active_outlined,
                    titre: t.mesPropositionsMission,
                    description: t.mesPropositionsMissionDescription,
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PropositionsMissionScreen())),
                  ),
                  const SizedBox(height: 12),
                  _CarteAction(
                    icone: Icons.assignment_outlined,
                    titre: t.mesMissions,
                    description: t.mesMissionsDescription,
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const MissionsScreen())),
                  ),
                  const SizedBox(height: 12),
                  _CarteAction(
                    icone: Icons.local_shipping_outlined,
                    titre: t.declarerCapacite,
                    description: t.declarerCapaciteDescription,
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CapaciteScreen())),
                  ),
                  const SizedBox(height: 12),
                  _CarteAction(
                    icone: Icons.garage_outlined,
                    titre: t.maFlotte,
                    description: t.maFlotteDescription,
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const VehiculesScreen())),
                  ),
                  const SizedBox(height: 12),
                  _CarteAction(
                    icone: Icons.account_balance_wallet_outlined,
                    titre: t.soldeEtGains,
                    description: t.soldeEtGainsDescription,
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PaiementScreen())),
                  ),
                  const SizedBox(height: 12),
                ],
                _CarteAction(
                  icone: Icons.route_outlined,
                  titre: t.axes,
                  description: t.axesDescription,
                  onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AxesScreen())),
                ),
                const SizedBox(height: 12),
                _CarteAction(
                  icone: Icons.badge_outlined,
                  titre: t.monProfil,
                  description: t.monProfilDescriptionChauffeur,
                  onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const KycScreen())),
                ),
              ]),
            ),
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
              label: Text(t.enroler, style: const TextStyle(color: AppColors.texteBouton, fontWeight: FontWeight.bold)),
            )
          : null,
    );
  }

  // Prénom (ou raison sociale pour un transporteur société) déclaré au KYC —
  // à défaut (profil pas encore complété), retombe sur le libellé du rôle.
  String _nomAffiche(AppLocalizations t, Profil? profil, String? role) {
    if (profil != null) {
      if (profil.type == 'ENTREPRISE' && (profil.raisonSociale?.isNotEmpty ?? false)) return profil.raisonSociale!;
      if (profil.prenom != null && profil.prenom!.isNotEmpty) return profil.prenom!;
    }
    return switch (role) {
      'CHAUFFEUR' => t.roleChauffeur,
      'TRANSPORTEUR' => t.roleTransporteur,
      'CHAUFFEUR_PROPRIETAIRE' => t.roleChauffeur,
      'AGENT' => t.roleAgent,
      _ => role ?? '—',
    };
  }
}

class _CarteAction extends StatelessWidget {
  final IconData icone;
  final String titre;
  final String description;
  final VoidCallback onTap;

  const _CarteAction({required this.icone, required this.titre, required this.description, required this.onTap});

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
              width: 44,
              height: 44,
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
