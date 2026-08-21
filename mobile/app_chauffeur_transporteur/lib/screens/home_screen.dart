import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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
import 'suivi_gps_screen.dart';
import 'vehicules_screen.dart';
import '../providers/notification_provider.dart';

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

    final nomAffiche = _nomAffiche(profil, authState.role);
    final initiale = nomAffiche.isNotEmpty ? nomAffiche[0].toUpperCase() : '?';

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
                            tooltip: 'Notifications',
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
                          Text('Bonjour, $nomAffiche',
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
                if (estTransporteur) ...[
                  _CarteAction(
                    icone: Icons.assignment_outlined,
                    titre: 'Mes missions',
                    description: 'Missions en cours et historique',
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const MissionsScreen())),
                  ),
                  const SizedBox(height: 12),
                  _CarteAction(
                    icone: Icons.local_shipping_outlined,
                    titre: 'Déclarer une capacité',
                    description: 'Proposer un trajet et de la place disponible',
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CapaciteScreen())),
                  ),
                  const SizedBox(height: 12),
                  _CarteAction(
                    icone: Icons.garage_outlined,
                    titre: 'Ma flotte',
                    description: 'Gérer mes véhicules',
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const VehiculesScreen())),
                  ),
                  const SizedBox(height: 12),
                  _CarteAction(
                    icone: Icons.gps_fixed,
                    titre: 'Suivi GPS',
                    description: 'Partager ma position pendant une mission',
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SuiviGpsScreen())),
                  ),
                  const SizedBox(height: 12),
                  _CarteAction(
                    icone: Icons.account_balance_wallet_outlined,
                    titre: 'Solde et gains',
                    description: 'Consulter mes paiements',
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PaiementScreen())),
                  ),
                  const SizedBox(height: 12),
                ],
                _CarteAction(
                  icone: Icons.route_outlined,
                  titre: 'Axes',
                  description: 'Corridors disponibles',
                  onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AxesScreen())),
                ),
                const SizedBox(height: 12),
                _CarteAction(
                  icone: Icons.badge_outlined,
                  titre: 'Mon profil',
                  description: 'Identité et niveau KYC',
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
              label: const Text('Enrôler', style: TextStyle(color: AppColors.texteBouton, fontWeight: FontWeight.bold)),
            )
          : null,
    );
  }

  // Prénom (ou raison sociale pour un transporteur société) déclaré au KYC —
  // à défaut (profil pas encore complété), retombe sur le libellé du rôle.
  String _nomAffiche(Profil? profil, String? role) {
    if (profil != null) {
      if (profil.type == 'ENTREPRISE' && (profil.raisonSociale?.isNotEmpty ?? false)) return profil.raisonSociale!;
      if (profil.prenom != null && profil.prenom!.isNotEmpty) return profil.prenom!;
    }
    return switch (role) {
      'CHAUFFEUR' => 'Chauffeur',
      'TRANSPORTEUR' => 'Transporteur',
      'CHAUFFEUR_PROPRIETAIRE' => 'Chauffeur',
      'AGENT' => 'Agent',
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
