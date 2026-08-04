import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import 'login_screen.dart';

// Écran d'accueil temporaire — sera remplacé par l'écran marketplace (Sprint 4)
class HomePlaceholderScreen extends ConsumerWidget {
  const HomePlaceholderScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);

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
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.check_circle, color: AppColors.succes, size: 64),
              const SizedBox(height: 16),
              Text('Connecté ✅', style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 8),
              Text('Rôle(s) : ${authState.roles.join(", ")}',
                  style: const TextStyle(color: AppColors.texteMuet)),
              const SizedBox(height: 32),
              const Text(
                'Sprint 1 — Authentification terminée.\nLa publication de demande (marketplace) arrive au Sprint 4.',
                textAlign: TextAlign.center,
                style: TextStyle(color: AppColors.texteMuet, fontSize: 13),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
