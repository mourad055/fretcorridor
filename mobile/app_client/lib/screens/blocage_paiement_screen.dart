import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import '../models/demande_model.dart';
import '../theme/app_theme.dart';
import 'diffusion_screen.dart';
import 'suivi_screen.dart';

// Pre-autorisation puis capture (spec, decision du 15/10) : les fonds sont
// bloques quand le client valide l'intervalle, PAS prelevees tant qu'aucun
// chauffeur n'a accepte -- seul le prix du chauffeur qui accepte est
// capture, le reste est libere aussitot. Simule ici : service-pay n'a
// aujourd'hui qu'un adaptateur sandbox (PrestatairePaiementPort), aucun
// etat intermediaire de pre-autorisation (SequestreEtat n'a que DECLENCHE/
// LIBERE) -- donc PAS le vrai ecran PaiementScreen (choix de moyen de
// reglement post-mission, deja reel), un ecran distinct et clairement
// demonstratif.
class BlocagePaiementScreen extends StatefulWidget {
  final DemandeModel demande;
  final double prixBas;
  final double plafond;
  final double? prixChauffeurConfirme;

  const BlocagePaiementScreen({
    super.key,
    required this.demande,
    required this.prixBas,
    required this.plafond,
    this.prixChauffeurConfirme,
  });

  @override
  State<BlocagePaiementScreen> createState() => _BlocagePaiementScreenState();
}

class _BlocagePaiementScreenState extends State<BlocagePaiementScreen> {
  bool get _confirme => widget.prixChauffeurConfirme != null;

  String _fmt(double n) => n.round().toString().replaceAllMapped(
        RegExp(r'\B(?=(\d{3})+(?!\d))'),
        (m) => ' ',
      );

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        children: [
          Container(
            margin: const EdgeInsets.only(bottom: 4),
            child: Row(children: [
              Expanded(child: Container(height: 5, decoration: BoxDecoration(color: AppColors.accent, borderRadius: BorderRadius.circular(3)))),
              const SizedBox(width: 6),
              Expanded(
                  child: Container(
                      height: 5,
                      decoration: BoxDecoration(
                          color: _confirme ? AppColors.accent : AppColors.bordure, borderRadius: BorderRadius.circular(3)))),
            ]),
          ),
          const SizedBox(height: 20),
          Column(children: [
            Container(
              width: 76,
              height: 76,
              decoration: BoxDecoration(color: _confirme ? AppColors.succes : Colors.orange, shape: BoxShape.circle),
              child: Icon(_confirme ? Icons.check : Icons.lock_clock, color: Colors.white, size: 34),
            ),
            const SizedBox(height: 10),
            Text(_confirme ? t.etape2sur2 : t.etape1sur2,
                style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: _confirme ? AppColors.succes : Colors.orange.shade800, letterSpacing: 1)),
            const SizedBox(height: 6),
            Text(_confirme ? t.paiementConfirmeChauffeurEnRoute : t.fondsBloquesEnAttente,
                textAlign: TextAlign.center, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
          ]),
          const SizedBox(height: 20),
          if (!_confirme) ...[
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(color: Colors.orange.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(16)),
              child: Column(children: [
                _ligne(t.montantBloque, '${_fmt(widget.plafond)} F', accent: true),
                _ligne(t.preleveActuellement, '0 F'),
                _ligne(t.compteLabel, 'MTN Mobile Money'),
                _ligne(t.fourchetteAcceptee, '${_fmt(widget.prixBas)} – ${_fmt(widget.plafond)} F'),
              ]),
            ),
            const SizedBox(height: 16),
            _etapePuce('1', t.etapePaiement1),
            const SizedBox(height: 10),
            _etapePuce('2', t.etapePaiement2),
            const SizedBox(height: 10),
            _etapePuce('–', t.etapePaiement3, neutre: true),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              height: 54,
              child: ElevatedButton(
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => DiffusionScreen(demande: widget.demande, prixBas: widget.prixBas, plafond: widget.plafond),
                  ),
                ),
                style: ElevatedButton.styleFrom(backgroundColor: AppColors.accent, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14))),
                child: Text(t.lancerLaRecherche, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
              ),
            ),
          ] else ...[
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(color: AppColors.surface, border: Border.all(color: AppColors.bordure), borderRadius: BorderRadius.circular(16)),
              child: Row(children: [
                Container(
                  width: 44, height: 44,
                  decoration: const BoxDecoration(color: AppColors.surfaceClaire, shape: BoxShape.circle),
                  child: const Icon(Icons.person, color: AppColors.accent),
                ),
                const SizedBox(width: 12),
                const Expanded(child: Text('[Nom du chauffeur]', style: TextStyle(fontWeight: FontWeight.bold))),
              ]),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(color: AppColors.succes.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(16)),
              child: Column(children: [
                _ligne(t.montantBloque, '${_fmt(widget.plafond)} F', barre: true),
                _ligne(t.montantPreleve, '${_fmt(widget.prixChauffeurConfirme!)} F', accent: true),
                _ligne(t.montantDebloque, '+${_fmt(widget.plafond - widget.prixChauffeurConfirme!)} F'),
              ]),
            ),
            const SizedBox(height: 14),
            Text(t.sequestreNote, textAlign: TextAlign.center, style: const TextStyle(fontSize: 12, color: AppColors.texteMuet)),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              height: 54,
              child: ElevatedButton(
                onPressed: () => Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(builder: (_) => SuiviScreen(demandeId: widget.demande.id, demande: widget.demande)),
                ),
                style: ElevatedButton.styleFrom(backgroundColor: AppColors.accent, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14))),
                child: Text(t.suivreLeChauffeur, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _ligne(String label, String valeur, {bool accent = false, bool barre = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
        Text(label, style: const TextStyle(fontSize: 13, color: AppColors.texteMuet)),
        Text(valeur,
            style: TextStyle(
              fontSize: accent ? 18 : 13,
              fontWeight: FontWeight.bold,
              decoration: barre ? TextDecoration.lineThrough : null,
              color: barre ? AppColors.texteMuet : AppColors.texte,
            )),
      ]),
    );
  }

  Widget _etapePuce(String numero, String texte, {bool neutre = false}) {
    return Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Container(
        width: 22, height: 22,
        alignment: Alignment.center,
        decoration: BoxDecoration(color: neutre ? AppColors.bordure : AppColors.accent, shape: BoxShape.circle),
        child: Text(numero, style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: neutre ? AppColors.texteMuet : Colors.white)),
      ),
      const SizedBox(width: 10),
      Expanded(child: Text(texte, style: const TextStyle(fontSize: 13, color: AppColors.texteMuet))),
    ]);
  }
}
