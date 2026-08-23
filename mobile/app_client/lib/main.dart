import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'l10n/app_localizations.dart';
import 'theme/app_theme.dart';
import 'providers/auth_provider.dart';
import 'providers/locale_provider.dart';
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
    final locale = ref.watch(localeProvider);

    return MaterialApp(
      title: 'FretCorridor — Client',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.theme,
      locale: locale,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: AppLocalizations.supportedLocales,
      home: authState.estConnecte ? const HomePlaceholderScreen() : const WelcomeScreen(),
    );
  }
}
