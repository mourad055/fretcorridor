import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'theme/app_theme.dart';
import 'providers/auth_provider.dart';
import 'screens/welcome_screen.dart';
import 'screens/home_placeholder_screen.dart';

void main() {
  runApp(const ProviderScope(child: FretCorridorClientApp()));
}

class FretCorridorClientApp extends ConsumerWidget {
  const FretCorridorClientApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);

    return MaterialApp(
      title: 'FretCorridor — Client',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.theme,
      home: authState.estConnecte ? const HomePlaceholderScreen() : const WelcomeScreen(),
    );
  }
}
