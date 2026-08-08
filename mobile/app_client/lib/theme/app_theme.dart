import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

// Palette "Fret Corridor" — rouge/blanc, d'après les nouvelles maquettes
// (logo, dashboard agent, écran de connexion redessinés).
class AppColors {
  static const fond          = Color(0xFFF5F5F7);
  static const surface       = Color(0xFFFFFFFF);
  static const surfaceClaire = Color(0xFFFDECEC); // fond rouge très clair (encadrés info)
  static const bordure       = Color(0xFFE5E7EB);
  static const accent        = Color(0xFFDC2626); // rouge principal — boutons, onglet actif
  static const accentProfond = Color(0xFFB91C1C); // rouge foncé — états pressés
  static const marqueOrange  = Color(0xFFF59E0B); // conservé pour "Verrouillé" (statut secondaire)
  static const texte         = Color(0xFF111827);
  static const texteMuet     = Color(0xFF6B7280);
  static const texteBouton   = Colors.white;
  static const succes        = Color(0xFF15803D);
  static const erreur        = Color(0xFFDC2626);
}

class AppTheme {
  static ThemeData get theme {
    final base = ThemeData.light();
    return base.copyWith(
      scaffoldBackgroundColor: AppColors.fond,
      colorScheme: base.colorScheme.copyWith(
        surface: AppColors.surface,
        primary: AppColors.accent,
        error: AppColors.erreur,
      ),
      textTheme: GoogleFonts.workSansTextTheme(base.textTheme).copyWith(
        headlineLarge: GoogleFonts.fraunces(
          fontSize: 28, fontWeight: FontWeight.w600, color: AppColors.texte,
        ),
        headlineMedium: GoogleFonts.fraunces(
          fontSize: 20, fontWeight: FontWeight.w600, color: AppColors.texte,
        ),
        titleMedium: GoogleFonts.fraunces(
          fontSize: 16, fontWeight: FontWeight.w600, color: AppColors.texte,
        ),
        bodyMedium: GoogleFonts.workSans(fontSize: 14, color: AppColors.texte),
        bodySmall: GoogleFonts.workSans(fontSize: 12, color: AppColors.texteMuet),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.fond,
        elevation: 0,
        titleTextStyle: GoogleFonts.fraunces(
          fontSize: 18, fontWeight: FontWeight.w600, color: AppColors.texte,
        ),
      ),
    );
  }
}
