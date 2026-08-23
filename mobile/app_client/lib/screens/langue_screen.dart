import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/locale_provider.dart';
import '../theme/app_theme.dart';

class LangueScreen extends ConsumerWidget {
  const LangueScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = AppLocalizations.of(context);
    final selection = ref.watch(localeProvider).languageCode;

    final langues = [
      ('fr', t.langueFrancais),
      ('en', t.langueAnglais),
    ];

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.langueTitre)),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          for (final l in langues)
            Container(
              margin: const EdgeInsets.only(bottom: 10),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: selection == l.$1 ? AppColors.accent : AppColors.bordure),
              ),
              child: ListTile(
                onTap: () => ref.read(localeProvider.notifier).choisir(l.$1),
                title: Text(l.$2, style: const TextStyle(fontWeight: FontWeight.w600)),
                trailing: Icon(
                  selection == l.$1 ? Icons.check_circle : Icons.circle_outlined,
                  color: selection == l.$1 ? AppColors.accent : AppColors.bordure,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
