import 'package:flutter/material.dart';
import 'simple_page_screen.dart';

class PolitiqueConfidentialiteScreen extends StatelessWidget {
  const PolitiqueConfidentialiteScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const SimplePageScreen(
      titre: 'Politique de confidentialité',
      icone: Icons.privacy_tip_outlined,
      sections: [
        SimpleSection('Données collectées',
            'Nous collectons les informations nécessaires au fonctionnement de la plateforme : identité déclarée, pièce d\'identité, numéro de téléphone, et les informations liées à vos demandes et missions de transport.'),
        SimpleSection('Utilisation des données',
            'Ces données servent à vérifier votre identité (KYC), à assurer la mise en relation entre chargeurs et transporteurs, et à assurer le suivi des missions. Elles ne sont jamais vendues à des tiers.'),
        SimpleSection('Partage des données',
            'Vos informations de contact sont partagées uniquement avec la contrepartie d\'une mission acceptée (chargeur ↔ transporteur), dans la limite nécessaire à son exécution.'),
        SimpleSection('Conservation',
            'Les données sont conservées pendant la durée de votre compte actif, puis archivées conformément aux obligations légales applicables.'),
        SimpleSection('Vos droits',
            'Vous pouvez demander l\'accès, la correction ou la suppression de vos données personnelles via le centre d\'aide.'),
      ],
    );
  }
}
