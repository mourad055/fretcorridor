import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

// Gabarit générique pour les pages statiques du menu (Politique, CGU, etc.)
// — évite de dupliquer le header et la mise en page dans chaque écran.
class SimpleSection {
  final String titre;
  final String texte;
  const SimpleSection(this.titre, this.texte);
}

class SimplePageScreen extends StatelessWidget {
  final String titre;
  final IconData icone;
  final List<SimpleSection> sections;

  const SimplePageScreen({super.key, required this.titre, required this.icone, required this.sections});

  @override
  Widget build(BuildContext context) {
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
                      Icon(icone, color: Colors.white, size: 26),
                      const SizedBox(width: 12),
                      Text(titre, style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: Colors.white)),
                    ]),
                  ),
                ],
              ),
            ),
          ),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(20),
              itemCount: sections.length,
              itemBuilder: (context, i) {
                final s = sections[i];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(s.titre, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                      const SizedBox(height: 6),
                      Text(s.texte, style: const TextStyle(color: AppColors.texteMuet, fontSize: 13, height: 1.5)),
                    ],
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
