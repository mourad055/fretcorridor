import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import 'login_screen.dart';

// Preuve que le login fonctionne de bout en bout (gateway -> service-ida) :
// affiche le rôle et le tenant réellement résolus depuis le JWT. Les écrans
// métier (KYC, capacité, missions...) viennent aux sprints suivants.
class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(
        title: const Text('FretCorridor'),
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
