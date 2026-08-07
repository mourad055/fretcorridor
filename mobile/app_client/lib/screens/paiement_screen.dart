import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

// S8 — l'écran existe, mais la logique de paiement (grand livre miroir,
// séquestre) appartient à service-pay, module de Personne 2 (Web), pas
// construit ici. Pas de fausse logique financière — état d'attente honnête.
class PaiementScreen extends StatelessWidget {
  const PaiementScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Paiement')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.payments_outlined, color: AppColors.bordure, size: 56),
              const SizedBox(height: 16),
              Text('Paiement bientôt disponible', style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 8),
              const Text(
                'Le paiement s\'effectuera via un prestataire agréé, au moment '
                'où vous acceptez une proposition de transport. Cette étape '
                'est en cours de finalisation.',
                textAlign: TextAlign.center,
                style: TextStyle(color: AppColors.texteMuet, fontSize: 13),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
