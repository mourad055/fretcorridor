import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/axes_provider.dart';
import '../providers/capacite_provider.dart';
import '../providers/vehicule_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/top_notification.dart';
import 'vehicules_screen.dart';

// S4 (EF-CAP-03/07) : déclaration de capacité (véhicule, trajet, créneau).
// Le véhicule vient du registre réel de la flotte (S10) — voir
// vehicule_provider.dart. Plus d'identifiant généré localement (TODO fermé).
//
// BUG CORRIGE (retour utilisatrice 24/08) : etait un ecran plein
// (Navigator.push + Scaffold/AppBar), sans jamais se fermer apres une
// creation reussie (formulaire juste vide pour permettre d'en declarer une
// autre a la suite) -- l'utilisatrice attend un popup qui se ferme
// systematiquement, meme principe que _FormulaireVehicule
// (vehicules_screen.dart). Ouvert desormais via showModalBottomSheet
// (voir home_screen.dart et mes_capacites_screen.dart), Scaffold/AppBar
// retires au profit d'un simple Padding, le raccourci "Mes capacites" de
// l'ancienne AppBar est retire (deja accessible depuis l'ecran d'accueil).
class CapaciteScreen extends ConsumerStatefulWidget {
  // CRUD capacité (audit de suivi Mobile) : pas de vrai endpoint de
  // modification côté backend (changer le poids déjà partiellement
  // décrémenté demanderait de recalculer le résiduel) — "modifier"
  // redéclare une nouvelle capacité pré-remplie puis supprime l'ancienne,
  // même principe que la modification de demande côté app Client.
  final CapaciteDeclaree? capaciteAModifier;
  const CapaciteScreen({super.key, this.capaciteAModifier});

  @override
  ConsumerState<CapaciteScreen> createState() => _CapaciteScreenState();
}

class _CapaciteScreenState extends ConsumerState<CapaciteScreen> {
  final _formKey = GlobalKey<FormState>();
  final _poidsCtrl = TextEditingController();
  String? _axeId;
  String? _vehiculeId;
  DateTime? _dateDepart;

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(axesProvider.notifier).charger();
      ref.read(vehiculeProvider.notifier).chargerMesVehicules();
      final c = widget.capaciteAModifier;
      if (c != null) {
        setState(() {
          _axeId = c.axeId;
          _vehiculeId = c.vehiculeId;
          _poidsCtrl.text = c.poidsKg.toStringAsFixed(0);
          _dateDepart = c.dateDepart;
        });
      }
    });
  }

  @override
  void dispose() {
    _poidsCtrl.dispose();
    super.dispose();
  }

  Future<void> _choisirDate() async {
    final date = await showDatePicker(
      context: context,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 90)),
      initialDate: DateTime.now().add(const Duration(days: 1)),
    );
    if (date == null || !mounted) return;
    final heure = await showTimePicker(context: context, initialTime: TimeOfDay.now());
    if (heure == null) return;
    setState(() {
      _dateDepart = DateTime(date.year, date.month, date.day, heure.hour, heure.minute);
    });
  }

  Future<void> _declarer(List<VehiculeFlotte> vehicules) async {
    final t = AppLocalizations.of(context);
    if (!_formKey.currentState!.validate()) return;
    if (_axeId == null) {
      afficherNotification(context, message: t.choisissezUnAxe, couleur: AppColors.erreur, icone: Icons.error_outline);
      return;
    }
    if (_vehiculeId == null) {
      afficherNotification(context, message: t.choisissezUnVehicule, couleur: AppColors.erreur, icone: Icons.error_outline);
      return;
    }
    if (_dateDepart == null) {
      afficherNotification(context, message: t.choisissezUneDateDepart, couleur: AppColors.erreur, icone: Icons.error_outline);
      return;
    }

    final vehicule = vehicules.firstWhere((v) => v.id == _vehiculeId);
    await ref.read(capaciteProvider.notifier).declarer(
          vehicule: vehicule,
          axeId: _axeId!,
          poidsKg: double.parse(_poidsCtrl.text),
          dateDepart: _dateDepart!,
        );
    if (!mounted) return;
    // BUG CORRIGE : l'ancienne bannière restait affichée indéfiniment
    // (liée à capaciteProvider.derniereCapacite, jamais réinitialisé —
    // même piège copyWith(champ: null) que suivi_provider.dart). Une
    // notification transitoire ne pose pas ce problème par construction.
    final capacite = ref.read(capaciteProvider).derniereCapacite;
    if (capacite == null) return;

    final ancienne = widget.capaciteAModifier;
    if (ancienne != null) {
      // La nouvelle capacité est déjà déclarée à ce stade — supprimer
      // l'ancienne est un nettoyage, pas une condition de succès : ne pas
      // bloquer l'utilisateur si ça échoue, juste l'avertir.
      final succesSuppression = await ref.read(capaciteProvider.notifier).supprimer(ancienne.id);
      if (!mounted) return;
      afficherNotification(
        context,
        message: succesSuppression
            ? t.capaciteModifiee
            : t.nouvelleCapaciteAncienneEchouee,
        couleur: succesSuppression ? AppColors.succes : AppColors.erreur,
        icone: succesSuppression ? Icons.check_circle : Icons.error_outline,
      );
      Navigator.pop(context);
      return;
    }

    afficherNotification(
      context,
      message: capacite.publiee
          ? t.capacitePubliee(capacite.poidsTaxableKg.round().toString())
          : t.capaciteEnregistree,
      couleur: AppColors.succes,
      icone: Icons.check_circle,
    );
    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(capaciteProvider);
    final axesState = ref.watch(axesProvider);
    final vehiculeState = ref.watch(vehiculeProvider);
    final t = AppLocalizations.of(context);

    return Padding(
      padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: MediaQuery.of(context).viewInsets.bottom + 24),
      child: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(widget.capaciteAModifier == null ? t.declarerCapacite : t.modifierLaCapacite,
                  style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 20),
              if (state.erreur != null) _bandeauErreur(state.erreur!),
                const SizedBox(height: 8),

                _label(t.axeLabel),
                const SizedBox(height: 8),
                DropdownButtonFormField<String>(
                  initialValue: _axeId,
                  decoration: _decoration(),
                  hint: Text(axesState.chargement ? t.chargementEnCours : t.choisirUnAxe),
                  items: axesState.axes
                      .map((a) => DropdownMenuItem(value: a.id, child: Text('${a.origine} → ${a.destination}')))
                      .toList(),
                  onChanged: (v) => setState(() => _axeId = v),
                ),
                const SizedBox(height: 16),

                Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                  _label(t.vehiculeLabel),
                  TextButton(
                    onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const VehiculesScreen()))
                        .then((_) => ref.read(vehiculeProvider.notifier).chargerMesVehicules()),
                    child: Text(t.gererMaFlotte, style: const TextStyle(fontSize: 12)),
                  ),
                ]),
                const SizedBox(height: 8),
                vehiculeState.vehicules.isEmpty
                    ? Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AppColors.surface,
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: AppColors.bordure),
                        ),
                        child: Text(
                          vehiculeState.chargement ? t.chargementEnCours : t.aucunVehiculeAjoutezEnUn,
                          style: const TextStyle(color: AppColors.texteMuet, fontSize: 13),
                        ),
                      )
                    : DropdownButtonFormField<String>(
                        initialValue: _vehiculeId,
                        decoration: _decoration(),
                        hint: Text(t.choisirUnVehicule),
                        items: vehiculeState.vehicules
                            .map((v) => DropdownMenuItem(value: v.id, child: Text(v.typeVehicule)))
                            .toList(),
                        onChanged: (v) => setState(() => _vehiculeId = v),
                      ),
                const SizedBox(height: 16),

                _label(t.poidsDisponibleKgLabel),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _poidsCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: _decoration(hint: 'Ex : 9500', suffixe: 'kg'),
                  validator: (v) {
                    if (v == null || v.isEmpty) return t.champObligatoire;
                    final n = double.tryParse(v);
                    if (n == null || n <= 0) return t.nombreInvalide;
                    return null;
                  },
                ),
                const SizedBox(height: 16),

                _label(t.departLabel),
                const SizedBox(height: 8),
                InkWell(
                  onTap: _choisirDate,
                  child: InputDecorator(
                    decoration: _decoration(),
                    child: Text(
                      _dateDepart == null
                          ? t.choisirDateEtHeure
                          : '${_dateDepart!.day}/${_dateDepart!.month}/${_dateDepart!.year} — '
                              '${_dateDepart!.hour.toString().padLeft(2, '0')}:${_dateDepart!.minute.toString().padLeft(2, '0')}',
                      style: TextStyle(color: _dateDepart == null ? AppColors.texteMuet : AppColors.texte),
                    ),
                  ),
                ),
                const SizedBox(height: 20),

                SizedBox(
                  width: double.infinity,
                  height: 52,
                  child: ElevatedButton(
                    onPressed: state.chargement ? null : () => _declarer(vehiculeState.vehicules),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.accent,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: state.chargement
                        ? const SizedBox(height: 22, width: 22, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                        : Text(widget.capaciteAModifier == null ? t.declarerLaCapacite : t.enregistrerLesModifications,
                            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
                  ),
                ),
                const SizedBox(height: 32),
              ],
            ),
          ),
        ),
    );
  }

  Widget _label(String text) => Text(text,
      style: const TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600));

  InputDecoration _decoration({String? hint, String? suffixe}) => InputDecoration(
        hintText: hint,
        suffixText: suffixe,
        suffixStyle: const TextStyle(color: AppColors.texteMuet, fontWeight: FontWeight.w600),
        filled: true,
        fillColor: AppColors.surface,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.accent)),
      );

  Widget _bandeauErreur(String message) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.erreur.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
      ),
      child: Row(children: [
        const Icon(Icons.warning_amber, color: AppColors.erreur),
        const SizedBox(width: 10),
        Expanded(child: Text(message, style: const TextStyle(color: AppColors.erreur, fontSize: 13))),
      ]),
    );
  }
}
