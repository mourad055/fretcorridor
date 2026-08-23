import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import 'simple_page_screen.dart';

class ConditionsUtilisationScreen extends StatelessWidget {
  const ConditionsUtilisationScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return SimplePageScreen(
      titre: t.cguTitre,
      icone: Icons.description_outlined,
      sections: [
        SimpleSection(t.cguObjetTitre, t.cguObjetTexte),
        SimpleSection(t.cguCompteTitre, t.cguCompteTexte),
        SimpleSection(t.cguDemandesTitre, t.cguDemandesTexte),
        SimpleSection(t.cguResponsabilitesTitre, t.cguResponsabilitesTexte),
        SimpleSection(t.cguModificationTitre, t.cguModificationTexte),
      ],
    );
  }
}
