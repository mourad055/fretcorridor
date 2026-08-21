import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class LangueScreen extends StatefulWidget {
  const LangueScreen({super.key});

  @override
  State<LangueScreen> createState() => _LangueScreenState();
}

class _LangueScreenState extends State<LangueScreen> {
  String _selection = 'fr';

  static const _langues = [
    ('fr', 'Français', true),
    ('en', 'English', false),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Langue')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          for (final l in _langues)
            Container(
              margin: const EdgeInsets.only(bottom: 10),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: _selection == l.$1 ? AppColors.accent : AppColors.bordure),
              ),
              child: ListTile(
                onTap: l.$3 ? () => setState(() => _selection = l.$1) : null,
                title: Text(l.$2, style: const TextStyle(fontWeight: FontWeight.w600)),
                subtitle: l.$3 ? null : const Text('Bientôt disponible', style: TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                trailing: Icon(
                  _selection == l.$1 ? Icons.check_circle : Icons.circle_outlined,
                  color: _selection == l.$1 ? AppColors.accent : AppColors.bordure,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
