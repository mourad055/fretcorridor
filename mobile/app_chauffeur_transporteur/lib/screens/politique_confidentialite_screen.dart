import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import 'simple_page_screen.dart';

class PolitiqueConfidentialiteScreen extends StatelessWidget {
  const PolitiqueConfidentialiteScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return SimplePageScreen(
      titre: t.politiqueTitre,
      icone: Icons.privacy_tip_outlined,
      sections: [
        SimpleSection(t.politiqueDonneesTitre, t.politiqueDonneesTexte),
        SimpleSection(t.politiqueUtilisationTitre, t.politiqueUtilisationTexte),
        SimpleSection(t.politiquePartageTitre, t.politiquePartageTexte),
        SimpleSection(t.politiqueConservationTitre, t.politiqueConservationTexte),
        SimpleSection(t.politiqueDroitsTitre, t.politiqueDroitsTexte),
      ],
    );
  }
}
