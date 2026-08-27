import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/vehicule_provider.dart';
import '../theme/app_theme.dart';

// S10 : console de flotte simplifiée (mode transporteur étendu).
class VehiculesScreen extends ConsumerStatefulWidget {
  const VehiculesScreen({super.key});

  @override
  ConsumerState<VehiculesScreen> createState() => _VehiculesScreenState();
}

class _VehiculesScreenState extends ConsumerState<VehiculesScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(vehiculeProvider.notifier).chargerMesVehicules());
  }

  Future<void> _ouvrirFormulaireAjout() async {
    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.fond,
      builder: (_) => const _FormulaireVehicule(),
    );
  }

  // CRUD véhicule (retour utilisatrice 21/08) : voir le détail, modifier ou
  // supprimer un véhicule déjà déclaré, pas seulement déclarer/lister.
  Future<void> _ouvrirDetail(VehiculeFlotte v) async {
    final t = AppLocalizations.of(context);
    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.fond,
      builder: (sheetContext) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(v.typeVehicule, style: Theme.of(context).textTheme.headlineMedium),
            const SizedBox(height: 12),
            if (v.immatriculation != null) _ligneDetail(Icons.badge_outlined, v.immatriculation!),
            if (v.profilPoidsMaxTonnes != null)
              _ligneDetail(Icons.scale_outlined, t.poidsMaxLabel(v.profilPoidsMaxTonnes!.toStringAsFixed(1))),
            if (v.profilNombreEssieux != null)
              _ligneDetail(Icons.settings_input_component_outlined, t.essieuxLabel('${v.profilNombreEssieux}')),
            if (v.profilMatieresDangereuses)
              _ligneDetail(Icons.warning_amber, t.matieresDangereuses),
            const SizedBox(height: 20),
            Row(children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {
                    Navigator.pop(sheetContext);
                    showModalBottomSheet(
                      context: context,
                      isScrollControlled: true,
                      backgroundColor: AppColors.fond,
                      builder: (_) => _FormulaireVehicule(existant: v),
                    );
                  },
                  icon: const Icon(Icons.edit_outlined, color: AppColors.accent),
                  label: Text(t.modifier, style: const TextStyle(color: AppColors.accent)),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () async {
                    Navigator.pop(sheetContext);
                    await _confirmerSuppression(v);
                  },
                  style: OutlinedButton.styleFrom(side: const BorderSide(color: AppColors.erreur)),
                  icon: const Icon(Icons.delete_outline, color: AppColors.erreur),
                  label: Text(t.supprimer, style: const TextStyle(color: AppColors.erreur)),
                ),
              ),
            ]),
          ],
        ),
      ),
    );
  }

  Widget _ligneDetail(IconData icone, String texte) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(children: [
        Icon(icone, size: 16, color: AppColors.texteMuet),
        const SizedBox(width: 8),
        Text(texte, style: const TextStyle(fontSize: 13)),
      ]),
    );
  }

  Future<void> _confirmerSuppression(VehiculeFlotte v) async {
    final t = AppLocalizations.of(context);
    final confirme = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(t.supprimerCeVehicule),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: Text(t.annuler)),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(t.supprimer, style: const TextStyle(color: AppColors.erreur)),
          ),
        ],
      ),
    );
    if (confirme == true) {
      await ref.read(vehiculeProvider.notifier).supprimer(v.id);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(vehiculeProvider);
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.maFlotte)),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _ouvrirFormulaireAjout,
        backgroundColor: AppColors.accent,
        icon: const Icon(Icons.add, color: AppColors.texteBouton),
        label: Text(t.ajouter, style: const TextStyle(color: AppColors.texteBouton, fontWeight: FontWeight.bold)),
      ),
      body: RefreshIndicator(
        onRefresh: () => ref.read(vehiculeProvider.notifier).chargerMesVehicules(),
        child: state.chargement && state.vehicules.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null
                ? _erreur(state.erreur!)
                : state.vehicules.isEmpty
                    ? ListView(children: [
                        const SizedBox(height: 80),
                        Center(
                          child: Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 32),
                            child: Text(t.aucunVehiculeEnregistre,
                                textAlign: TextAlign.center, style: const TextStyle(color: AppColors.texteMuet)),
                          ),
                        ),
                      ])
                    : ListView.separated(
                        padding: const EdgeInsets.fromLTRB(16, 16, 16, 90),
                        itemCount: state.vehicules.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 10),
                        itemBuilder: (context, i) => _carteVehicule(state.vehicules[i]),
                      ),
      ),
    );
  }

  Widget _erreur(String message) {
    return ListView(children: [
      const SizedBox(height: 80),
      const Center(child: Icon(Icons.wifi_off, color: AppColors.texteMuet, size: 48)),
      const SizedBox(height: 12),
      Center(child: Text(message, style: const TextStyle(color: AppColors.texteMuet))),
    ]);
  }

  Widget _carteVehicule(VehiculeFlotte v) {
    return InkWell(
      onTap: () => _ouvrirDetail(v),
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.bordure),
        ),
        child: Row(children: [
          const Icon(Icons.local_shipping_outlined, color: AppColors.accent),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(v.typeVehicule, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                if (v.immatriculation != null)
                  Text(v.immatriculation!, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
              ],
            ),
          ),
          if (v.profilMatieresDangereuses)
            const Padding(
              padding: EdgeInsets.only(right: 6),
              child: Icon(Icons.warning_amber, color: AppColors.accent, size: 18),
            ),
          const Icon(Icons.chevron_right, color: AppColors.texteMuet),
        ]),
      ),
    );
  }
}

// CRUD véhicule (retour utilisatrice 21/08) : même formulaire pour créer et
// modifier -- `existant` non-null bascule en mode édition (titre, appel
// modifier() plutôt que declarer(), champs pré-remplis). Reste un popup qui
// se ferme après soumission (showModalBottomSheet + Navigator.pop), déjà le
// cas pour la création.
class _FormulaireVehicule extends ConsumerStatefulWidget {
  final VehiculeFlotte? existant;
  const _FormulaireVehicule({this.existant});

  @override
  ConsumerState<_FormulaireVehicule> createState() => _FormulaireVehiculeState();
}

class _FormulaireVehiculeState extends ConsumerState<_FormulaireVehicule> {
  final _formKey = GlobalKey<FormState>();
  late final _typeCtrl = TextEditingController(text: widget.existant?.typeVehicule);
  late final _immatCtrl = TextEditingController(text: widget.existant?.immatriculation);
  late final _poidsMaxCtrl =
      TextEditingController(text: widget.existant?.profilPoidsMaxTonnes?.toString());
  late final _essieuxCtrl =
      TextEditingController(text: widget.existant?.profilNombreEssieux?.toString());
  late bool _matieresDangereuses = widget.existant?.profilMatieresDangereuses ?? false;

  bool get _modeEdition => widget.existant != null;

  @override
  void dispose() {
    _typeCtrl.dispose();
    _immatCtrl.dispose();
    _poidsMaxCtrl.dispose();
    _essieuxCtrl.dispose();
    super.dispose();
  }

  Future<void> _valider() async {
    if (!_formKey.currentState!.validate()) return;
    final notifier = ref.read(vehiculeProvider.notifier);
    final succes = _modeEdition
        ? await notifier.modifier(
            id: widget.existant!.id,
            typeVehicule: _typeCtrl.text.trim(),
            immatriculation: _immatCtrl.text.trim().isEmpty ? null : _immatCtrl.text.trim(),
            profilPoidsMaxTonnes: double.tryParse(_poidsMaxCtrl.text),
            profilNombreEssieux: int.tryParse(_essieuxCtrl.text),
            profilMatieresDangereuses: _matieresDangereuses,
          )
        : await notifier.declarer(
            typeVehicule: _typeCtrl.text.trim(),
            immatriculation: _immatCtrl.text.trim().isEmpty ? null : _immatCtrl.text.trim(),
            profilPoidsMaxTonnes: double.tryParse(_poidsMaxCtrl.text),
            profilNombreEssieux: int.tryParse(_essieuxCtrl.text),
            profilMatieresDangereuses: _matieresDangereuses,
          );
    if (succes && mounted) Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(vehiculeProvider);
    final t = AppLocalizations.of(context);

    return Padding(
      padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: MediaQuery.of(context).viewInsets.bottom + 24),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(_modeEdition ? t.modifierLeVehicule : t.nouveauVehicule,
                style: Theme.of(context).textTheme.headlineMedium),
            const SizedBox(height: 20),
            TextFormField(
              controller: _typeCtrl,
              decoration: InputDecoration(labelText: t.typeDeVehicule),
              validator: (v) => (v == null || v.trim().isEmpty) ? t.champObligatoire : null,
            ),
            const SizedBox(height: 12),
            TextFormField(controller: _immatCtrl, decoration: InputDecoration(labelText: t.immatriculationFacultatif)),
            const SizedBox(height: 12),
            TextFormField(
              controller: _poidsMaxCtrl,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: InputDecoration(labelText: t.poidsMaxTonnesFacultatif),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _essieuxCtrl,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(labelText: t.nombreEssieuxFacultatif),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(t.matieresDangereuses),
              value: _matieresDangereuses,
              onChanged: (v) => setState(() => _matieresDangereuses = v),
              activeThumbColor: AppColors.accent,
            ),
            if (state.erreur != null) ...[
              const SizedBox(height: 8),
              Text(state.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 13)),
            ],
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton(
                onPressed: state.chargement ? null : _valider,
                style: ElevatedButton.styleFrom(backgroundColor: AppColors.accent),
                child: state.chargement
                    ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                    : Text(t.enregistrer, style: const TextStyle(color: AppColors.texteBouton, fontWeight: FontWeight.bold)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
