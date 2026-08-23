import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import '../theme/app_theme.dart';

class AideScreen extends StatelessWidget {
  const AideScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final questions = [
      (t.aideQ1, t.aideR1),
      (t.aideQ2, t.aideR2),
      (t.aideQ3, t.aideR3),
      (t.aideQ4, t.aideR4),
    ];

    return Scaffold(
      backgroundColor: AppColors.fond,
      body: Column(
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(4, 0, 20, 24),
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
                  IconButton(
                    icon: const Icon(Icons.arrow_back, color: Colors.white),
                    onPressed: () => Navigator.pop(context),
                  ),
                  Padding(
                    padding: const EdgeInsets.only(left: 16, top: 4),
                    child: Row(children: [
                      const Icon(Icons.help_outline, color: Colors.white, size: 26),
                      const SizedBox(width: 12),
                      Text(t.centreAide, style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: Colors.white)),
                    ]),
                  ),
                ],
              ),
            ),
          ),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(20),
              children: [
                Text(t.aideFaqTitre,
                    style: const TextStyle(fontSize: 11, letterSpacing: 1.1, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
                const SizedBox(height: 12),
                ...questions.map((q) => Container(
                      margin: const EdgeInsets.only(bottom: 10),
                      decoration: BoxDecoration(
                        color: AppColors.surface,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: AppColors.bordure),
                      ),
                      child: Theme(
                        data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
                        child: ExpansionTile(
                          title: Text(q.$1, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                          childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                          expandedCrossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(q.$2, style: const TextStyle(color: AppColors.texteMuet, fontSize: 13, height: 1.5)),
                          ],
                        ),
                      ),
                    )),
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceClaire,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(children: [
                    const Icon(Icons.support_agent, color: AppColors.accent),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(t.aideContact,
                          style: const TextStyle(fontSize: 13, color: AppColors.texte)),
                    ),
                  ]),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
