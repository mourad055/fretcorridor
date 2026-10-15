import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import '../models/demande_model.dart';
import '../theme/app_theme.dart';
import 'blocage_paiement_screen.dart';

// Tarification par chauffeur (spec-tarification-par-chauffeur.md) : prix =
// poids taxable x tarif du chauffeur, par axe et categorie. L'intervalle
// affiche au client = min/max parmi les chauffeurs actuellement compatibles.
// Aucun endpoint backend ne calcule encore cet intervalle (le Moteur doit
// adapter TarificationL4Service) -- ecran 100% simule a partir du poids
// taxable REEL de la demande, en attendant ce contrat.
class EstimationScreen extends StatefulWidget {
  final DemandeModel demande;
  const EstimationScreen({super.key, required this.demande});

  @override
  State<EstimationScreen> createState() => _EstimationScreenState();
}

class _EstimationScreenState extends State<EstimationScreen> {
  // Tarifs de demonstration (F/kg) -- 3 "chauffeurs compatibles" fictifs,
  // meme fourchette que celle utilisee sur la maquette Design validee.
  static const _tarifsDemo = [75.0, 95.0, 115.0];

  bool _negociation = false;
  late double _plafond;

  double get _poids => widget.demande.poidsTaxableKg;
  List<double> get _prix => _tarifsDemo.map((t) => t * _poids).toList()..sort();
  double get _prixBas => _prix.first;
  double get _prixHaut => _prix.last;

  @override
  void initState() {
    super.initState();
    _plafond = _prixHaut;
  }

  String _fmt(double n) => n.round().toString().replaceAllMapped(
        RegExp(r'\B(?=(\d{3})+(?!\d))'),
        (m) => ' ',
      );

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final nbCompatibles = _prix.where((p) => p <= _plafond).length;
    final avertissement = _negociation && nbCompatibles == 0;

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.estimationTitre)),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(color: AppColors.accent, borderRadius: BorderRadius.circular(18)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(t.fourchetteDePrix,
                    style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1)),
                const SizedBox(height: 6),
                Text(t.entre, style: const TextStyle(color: Colors.white70, fontSize: 14)),
                Text('${_fmt(_prixBas)} – ${_fmt(_negociation ? _plafond : _prixHaut)} FCFA',
                    style: const TextStyle(color: Colors.white, fontSize: 26, fontWeight: FontWeight.bold)),
                const SizedBox(height: 10),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.18), borderRadius: BorderRadius.circular(20)),
                  child: Text(t.chauffeursCompatibles(_tarifsDemo.length),
                      style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.bordure),
            ),
            child: Column(children: [
              _ligneDetail(t.poidsTaxableLabel, '${_poids.toStringAsFixed(0)} kg'),
              const SizedBox(height: 8),
              _ligneDetail(t.tarifsDesChauffeurs, '${_tarifsDemo.first.round()} – ${_tarifsDemo.last.round()} F/kg'),
              const Divider(height: 24),
              _ligneDetail(t.prixDunChauffeur, t.poidsFoisTarif),
            ]),
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: _negociation ? AppColors.accent : AppColors.bordure, width: _negociation ? 1.5 : 1),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                InkWell(
                  onTap: () => setState(() => _negociation = !_negociation),
                  child: Row(children: [
                    Expanded(
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text(t.negocierLaFourchette, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                        Text(t.negocierFacultatif, style: const TextStyle(fontSize: 12, color: AppColors.texteMuet)),
                      ]),
                    ),
                    Switch(value: _negociation, activeTrackColor: AppColors.accent, onChanged: (v) => setState(() => _negociation = v)),
                  ]),
                ),
                if (_negociation) ...[
                  const SizedBox(height: 10),
                  Text(t.votrePrixMaximum, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                  Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                    IconButton(
                      onPressed: () => setState(() => _plafond = (_plafond - 2500).clamp(_prixBas, _prixHaut)),
                      icon: const Icon(Icons.remove_circle_outline),
                    ),
                    SizedBox(
                      width: 110,
                      child: Text('${_fmt(_plafond)} F', textAlign: TextAlign.center,
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                    ),
                    IconButton(
                      onPressed: () => setState(() => _plafond = (_plafond + 2500).clamp(_prixBas, _prixHaut)),
                      icon: const Icon(Icons.add_circle_outline, color: AppColors.accent),
                    ),
                  ]),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: avertissement ? Colors.orange.withValues(alpha: 0.12) : AppColors.surfaceClaire,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      avertissement ? t.negoAvertissement : t.negoInfo(_fmt(_plafond)),
                      style: TextStyle(fontSize: 12, color: avertissement ? Colors.orange.shade800 : AppColors.texte),
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(height: 14),
          Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Icon(Icons.verified_outlined, color: AppColors.accent, size: 18),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                t.validationFondsBloques(_fmt(_prixBas), _fmt(_negociation ? _plafond : _prixHaut)),
                style: const TextStyle(fontSize: 12, color: AppColors.texteMuet),
              ),
            ),
          ]),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            height: 54,
            child: ElevatedButton(
              onPressed: avertissement
                  ? null
                  : () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => BlocagePaiementScreen(
                            demande: widget.demande,
                            prixBas: _prixBas,
                            plafond: _negociation ? _plafond : _prixHaut,
                          ),
                        ),
                      ),
              style: ElevatedButton.styleFrom(backgroundColor: AppColors.accent, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14))),
              child: Text(t.validerEtRechercherChauffeur, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _ligneDetail(String label, String valeur) {
    return Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
      Text(label, style: const TextStyle(color: AppColors.texteMuet, fontSize: 13)),
      Text(valeur, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
    ]);
  }
}
