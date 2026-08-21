import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import 'aide_screen.dart';
import 'completer_profil_screen.dart';
import 'conditions_utilisation_screen.dart';
import 'langue_screen.dart';
import 'login_screen.dart';
import 'notifications_screen.dart';
import 'parametres_screen.dart';
import 'politique_confidentialite_screen.dart';

// Menu latéral (icône hamburger de l'accueil) — même structure que l'app
// Chauffeur/Transporteur pour une identité cohérente entre les deux apps.
class MenuDrawer extends ConsumerWidget {
  const MenuDrawer({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);

    return Drawer(
      backgroundColor: AppColors.surface,
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 20, 12, 12),
              child: Row(
                children: [
                  Container(
                    width: 48, height: 48,
                    decoration: const BoxDecoration(color: AppColors.surfaceClaire, shape: BoxShape.circle),
                    child: const Icon(Icons.person, color: AppColors.accent),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Menu', style: Theme.of(context).textTheme.titleMedium),
                        Text(authState.telephone ?? 'Espace utilisateur',
                            style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                      ],
                    ),
                  ),
                  IconButton(icon: const Icon(Icons.close, color: AppColors.texteMuet), onPressed: () => Navigator.pop(context)),
                ],
              ),
            ),
            const Divider(height: 1),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.symmetric(vertical: 8),
                children: [
                  _item(context, Icons.person_outline, 'Profil',
                      () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CompleterProfilScreen()))),
                  _item(context, Icons.notifications_none, 'Notifications',
                      () => Navigator.push(context, MaterialPageRoute(builder: (_) => const NotificationsScreen()))),
                  _item(context, Icons.language, 'Langue (Français)',
                      () => Navigator.push(context, MaterialPageRoute(builder: (_) => const LangueScreen()))),
                  _item(context, Icons.help_outline, 'Centre d\'aide',
                      () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AideScreen()))),
                  _item(context, Icons.privacy_tip_outlined, 'Politique & confidentialité',
                      () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PolitiqueConfidentialiteScreen()))),
                  _item(context, Icons.description_outlined, 'Conditions d\'utilisation',
                      () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ConditionsUtilisationScreen()))),
                  _item(context, Icons.settings_outlined, 'Paramètres',
                      () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ParametresScreen()))),
                ],
              ),
            ),
            const Divider(height: 1),
            _item(context, Icons.logout, 'Se déconnecter', () async {
              await ref.read(authProvider.notifier).logout();
              if (context.mounted) {
                Navigator.pushAndRemoveUntil(context, MaterialPageRoute(builder: (_) => const LoginScreen()), (route) => false);
              }
            }, couleur: AppColors.erreur),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  Widget _item(BuildContext context, IconData icone, String titre, VoidCallback? onTap, {Color? couleur}) {
    return ListTile(
      leading: Icon(icone, color: couleur ?? AppColors.texte),
      title: Text(titre, style: TextStyle(color: couleur ?? AppColors.texte, fontSize: 14)),
      onTap: onTap == null
          ? null
          : () {
              Navigator.pop(context);
              onTap();
            },
      enabled: onTap != null,
    );
  }
}
