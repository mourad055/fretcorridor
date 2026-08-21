import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'theme/app_theme.dart';
import 'providers/auth_provider.dart';
import 'screens/welcome_screen.dart';
import 'screens/home_screen.dart';

void main() {
  runApp(const ProviderScope(child: FretCorridorChauffeurApp()));
}

class FretCorridorChauffeurApp extends ConsumerWidget {
  const FretCorridorChauffeurApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);

    return MaterialApp(
      title: 'FretCorridor — Chauffeur/Transporteur',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.theme,
      home: authState.estConnecte ? const HomeScreen() : const WelcomeScreen(),
    );
  }
}
