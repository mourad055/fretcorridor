import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class AideScreen extends StatelessWidget {
  const AideScreen({super.key});

  static const _questions = [
    ('Comment publier une demande de transport ?',
        'Depuis l\'accueil, appuyez sur "Envoyer une marchandise", renseignez le lieu, le type et la quantité de marchandise, puis les informations du destinataire.'),
    ('Pourquoi mon profil doit être complété ?',
        'La complétion du profil (identité + pièce d\'identité) est obligatoire avant de pouvoir publier une demande — c\'est une exigence de vérification (KYC).'),
    ('Le prix affiché est-il définitif ?',
        'Non, le prix affiché lors de la publication est une estimation. Le prix définitif est celui de la proposition que vous acceptez.'),
    ('Comment suivre mes demandes ?',
        'Rendez-vous sur "Mes demandes" depuis l\'accueil pour voir le statut de chaque demande et ses propositions reçues.'),
  ];

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
                      const Icon(Icons.help_outline, color: Colors.white, size: 26),
                      const SizedBox(width: 12),
                      Text('Centre d\'aide', style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: Colors.white)),
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
                const Text('QUESTIONS FRÉQUENTES',
                    style: TextStyle(fontSize: 11, letterSpacing: 1.1, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
                const SizedBox(height: 12),
                ..._questions.map((q) => Container(
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
                    const Expanded(
                      child: Text('Besoin d\'aide supplémentaire ? Contactez l\'agence FretCorridor la plus proche.',
                          style: TextStyle(fontSize: 13, color: AppColors.texte)),
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
