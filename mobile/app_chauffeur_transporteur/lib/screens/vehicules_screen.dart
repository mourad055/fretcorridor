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
      builder: (_) => const _FormulaireAjoutVehicule(),
    );
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
    return Container(
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
          const Icon(Icons.warning_amber, color: AppColors.accent, size: 18),
      ]),
    );
  }
}

class _FormulaireAjoutVehicule extends ConsumerStatefulWidget {
  const _FormulaireAjoutVehicule();

  @override
  ConsumerState<_FormulaireAjoutVehicule> createState() => _FormulaireAjoutVehiculeState();
}

class _FormulaireAjoutVehiculeState extends ConsumerState<_FormulaireAjoutVehicule> {
  final _formKey = GlobalKey<FormState>();
  final _typeCtrl = TextEditingController();
  final _immatCtrl = TextEditingController();
  final _poidsMaxCtrl = TextEditingController();
  final _essieuxCtrl = TextEditingController();
  bool _matieresDangereuses = false;

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
    final succes = await ref.read(vehiculeProvider.notifier).declarer(
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
            Text(t.nouveauVehicule, style: Theme.of(context).textTheme.headlineMedium),
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
