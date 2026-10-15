import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import '../models/demande_model.dart';
import '../theme/app_theme.dart';
import 'blocage_paiement_screen.dart';
import 'estimation_screen.dart';

// Diffusion-course (ADR 0019) : la demande est envoyee a tous les
// chauffeurs compatibles en meme temps, premier qui accepte gagne. Compte
// a rebours de 15 min, cote cette fois sur le vrai timeout configurable
// d'OPT (fretcorridor.opt.proposition-expiration-ms, defaut 900000ms) --
// ecran 100% simule, aucun polling reel du statut de diffusion n'existe
// encore cote backend pour le client.
class DiffusionScreen extends StatefulWidget {
  final DemandeModel demande;
  final double prixBas;
  final double plafond;

  const DiffusionScreen({super.key, required this.demande, required this.prixBas, required this.plafond});

  @override
  State<DiffusionScreen> createState() => _DiffusionScreenState();
}

class _DiffusionScreenState extends State<DiffusionScreen> {
  static const _dureeTotale = 900;
  late int _restant;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _demarrer();
  }

  void _demarrer() {
    _timer?.cancel();
    setState(() => _restant = _dureeTotale);
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      setState(() => _restant = max(0, _restant - 1));
      if (_restant == 0) _timer?.cancel();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  String _fmt(double n) => n.round().toString().replaceAllMapped(
        RegExp(r'\B(?=(\d{3})+(?!\d))'),
        (m) => ' ',
      );

  void _unChauffeurAccepte() {
    final r = Random();
    final prix = widget.prixBas + r.nextDouble() * (widget.plafond - widget.prixBas);
    _timer?.cancel();
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (_) => BlocagePaiementScreen(
          demande: widget.demande,
          prixBas: widget.prixBas,
          plafond: widget.plafond,
          prixChauffeurConfirme: prix,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final expire = _restant == 0;
    final minutes = (_restant ~/ 60).toString().padLeft(2, '0');
    final secondes = (_restant % 60).toString().padLeft(2, '0');

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.diffusionTitre)),
      body: expire ? _vueExpiree(t) : _vueAttente(t, minutes, secondes),
    );
  }

  Widget _vueAttente(AppLocalizations t, String minutes, String secondes) {
    final progression = _restant / _dureeTotale;
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(children: [
        const SizedBox(height: 20),
        SizedBox(
          width: 170,
          height: 170,
          child: Stack(alignment: Alignment.center, children: [
            SizedBox(
              width: 170,
              height: 170,
              child: CircularProgressIndicator(
                value: progression,
                strokeWidth: 8,
                backgroundColor: AppColors.bordure,
                valueColor: const AlwaysStoppedAnimation(AppColors.accent),
              ),
            ),
            Column(mainAxisSize: MainAxisSize.min, children: [
              Text(t.expireDansLabel, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.texteMuet)),
              Text('$minutes:$secondes', style: const TextStyle(fontSize: 34, fontWeight: FontWeight.bold)),
              Text(t.sur1500, style: const TextStyle(fontSize: 12, color: AppColors.texteMuet)),
            ]),
          ]),
        ),
        const SizedBox(height: 24),
        Text(t.rechercheChauffeurEnCours, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold), textAlign: TextAlign.center),
        const SizedBox(height: 10),
        Text(t.diffusionDescription, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.texteMuet, fontSize: 14)),
        const SizedBox(height: 16),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          decoration: BoxDecoration(color: AppColors.surface, border: Border.all(color: AppColors.bordure), borderRadius: BorderRadius.circular(20)),
          child: Text('${t.fourchetteAcceptee} · ${_fmt(widget.prixBas)} – ${_fmt(widget.plafond)} F',
              style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
        ),
        const Spacer(),
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(color: Colors.orange.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(14)),
          child: Row(children: [
            Icon(Icons.lock_clock, color: Colors.orange.shade800, size: 20),
            const SizedBox(width: 10),
            Expanded(
              child: Text('${t.montantBloque} · ${_fmt(widget.plafond)} F',
                  style: TextStyle(fontSize: 12, color: Colors.orange.shade800, fontWeight: FontWeight.w600)),
            ),
          ]),
        ),
        const SizedBox(height: 12),
        Row(children: [
          Expanded(
            child: OutlinedButton(
              onPressed: () => Navigator.popUntil(context, (route) => route.isFirst),
              child: Text(t.annulerLaDemande),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: ElevatedButton(
              onPressed: _unChauffeurAccepte,
              style: ElevatedButton.styleFrom(backgroundColor: AppColors.accent),
              child: Text(t.demoChauffeurAccepte, style: const TextStyle(color: Colors.white, fontSize: 12), textAlign: TextAlign.center),
            ),
          ),
        ]),
      ]),
    );
  }

  Widget _vueExpiree(AppLocalizations t) {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
        const Icon(Icons.hourglass_bottom, size: 56, color: AppColors.texteMuet),
        const SizedBox(height: 16),
        Text(t.offreExpireeTitre, textAlign: TextAlign.center, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
        const SizedBox(height: 10),
        Text(t.offreExpireeDescription, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.texteMuet, fontSize: 14)),
        const SizedBox(height: 24),
        SizedBox(
          width: double.infinity,
          height: 54,
          child: ElevatedButton(
            onPressed: _demarrer,
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.accent, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14))),
            child: Text(t.republier, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
          ),
        ),
        const SizedBox(height: 10),
        SizedBox(
          width: double.infinity,
          height: 48,
          child: OutlinedButton(
            onPressed: () => Navigator.pushReplacement(
              context,
              MaterialPageRoute(builder: (_) => EstimationScreen(demande: widget.demande)),
            ),
            child: Text(t.revoirMonPrix),
          ),
        ),
      ]),
    );
  }
}
