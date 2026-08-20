import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Ma flotte')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _ouvrirFormulaireAjout,
        backgroundColor: AppColors.accent,
        icon: const Icon(Icons.add, color: AppColors.texteBouton),
        label: const Text('Ajouter', style: TextStyle(color: AppColors.texteBouton, fontWeight: FontWeight.bold)),
      ),
      body: RefreshIndicator(
        onRefresh: () => ref.read(vehiculeProvider.notifier).chargerMesVehicules(),
        child: state.chargement && state.vehicules.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null
                ? _erreur(state.erreur!)
                : state.vehicules.isEmpty
                    ? ListView(children: const [
                        SizedBox(height: 80),
                        Center(
                          child: Padding(
                            padding: EdgeInsets.symmetric(horizontal: 32),
                            child: Text('Aucun véhicule enregistré.\nAppuyez sur "Ajouter" pour en déclarer un.',
                                textAlign: TextAlign.center, style: TextStyle(color: AppColors.texteMuet)),
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

    return Padding(
      padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: MediaQuery.of(context).viewInsets.bottom + 24),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Nouveau véhicule', style: Theme.of(context).textTheme.headlineMedium),
            const SizedBox(height: 20),
            TextFormField(
              controller: _typeCtrl,
              decoration: const InputDecoration(labelText: 'Type de véhicule'),
              validator: (v) => (v == null || v.trim().isEmpty) ? 'Champ obligatoire' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(controller: _immatCtrl, decoration: const InputDecoration(labelText: 'Immatriculation (facultatif)')),
            const SizedBox(height: 12),
            TextFormField(
              controller: _poidsMaxCtrl,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: 'Poids max (tonnes, facultatif)'),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _essieuxCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Nombre d\'essieux (facultatif)'),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Matières dangereuses'),
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
                    : const Text('Enregistrer', style: TextStyle(color: AppColors.texteBouton, fontWeight: FontWeight.bold)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
