import 'package:flutter/material.dart';
import 'simple_page_screen.dart';

class ConditionsUtilisationScreen extends StatelessWidget {
  const ConditionsUtilisationScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const SimplePageScreen(
      titre: 'Conditions d\'utilisation',
      icone: Icons.description_outlined,
      sections: [
        SimpleSection('Objet',
            'FretCorridor met en relation des chargeurs et des transporteurs pour l\'organisation de transports de marchandises. La plateforme ne réalise pas elle-même les transports.'),
        SimpleSection('Compte utilisateur',
            'Vous êtes responsable de l\'exactitude des informations fournies lors de l\'inscription et de la complétion de votre profil (KYC). Un compte peut être suspendu en cas d\'information frauduleuse.'),
        SimpleSection('Demandes et propositions',
            'Toute demande publiée peut recevoir jusqu\'à 3 propositions classées. Le prix affiché avant acceptation est une estimation ; le prix définitif est fixé au moment de l\'acceptation d\'une proposition.'),
        SimpleSection('Responsabilités',
            'Le chargeur est responsable de l\'exactitude des informations sur la marchandise (poids, nature, destinataire). Le transporteur est responsable de la bonne exécution de la mission acceptée.'),
        SimpleSection('Modification',
            'Ces conditions peuvent évoluer ; les utilisateurs seront informés des changements significatifs via l\'application.'),
      ],
    );
  }
}
