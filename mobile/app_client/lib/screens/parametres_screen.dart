import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import '../theme/app_theme.dart';
import 'langue_screen.dart';

class ParametresScreen extends StatefulWidget {
  const ParametresScreen({super.key});

  @override
  State<ParametresScreen> createState() => _ParametresScreenState();
}

class _ParametresScreenState extends State<ParametresScreen> {
  bool _notificationsPush = true;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.parametres)),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text(t.sectionNotifications,
              style: const TextStyle(fontSize: 11, letterSpacing: 1.1, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          Container(
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppColors.bordure),
            ),
            child: SwitchListTile(
              activeThumbColor: AppColors.accent,
              value: _notificationsPush,
              onChanged: (v) => setState(() => _notificationsPush = v),
              title: Text(t.notificationsPush, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
              subtitle: Text(t.notificationsPushDescription, style: const TextStyle(fontSize: 12, color: AppColors.texteMuet)),
            ),
          ),
          const SizedBox(height: 24),

          Text(t.sectionGeneral,
              style: const TextStyle(fontSize: 11, letterSpacing: 1.1, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          Container(
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppColors.bordure),
            ),
            child: ListTile(
              leading: const Icon(Icons.language, color: AppColors.texte),
              title: Text(t.langue, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
              trailing: const Icon(Icons.chevron_right, color: AppColors.texteMuet),
              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const LangueScreen())),
            ),
          ),
          const SizedBox(height: 24),

          Center(
            child: Text(t.versionApp,
                style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
          ),
        ],
      ),
    );
  }
}
