import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/axes_provider.dart';
import '../providers/capacite_provider.dart';
import '../providers/vehicule_provider.dart';
import '../theme/app_theme.dart';
import 'vehicules_screen.dart';

// S4 (EF-CAP-03/07) : déclaration de capacité (véhicule, trajet, créneau).
// Le véhicule vient du registre réel de la flotte (S10) — voir
// vehicule_provider.dart. Plus d'identifiant généré localement (TODO fermé).
class CapaciteScreen extends ConsumerStatefulWidget {
  const CapaciteScreen({super.key});

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
    if (!_formKey.currentState!.validate()) return;
    if (_axeId == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Choisissez un axe.')));
      return;
    }
    if (_vehiculeId == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Choisissez un véhicule.')));
      return;
    }
    if (_dateDepart == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Choisissez une date de départ.')));
      return;
    }

    final vehicule = vehicules.firstWhere((v) => v.id == _vehiculeId);
    await ref.read(capaciteProvider.notifier).declarer(
          vehicule: vehicule,
          axeId: _axeId!,
          poidsKg: double.parse(_poidsCtrl.text),
          dateDepart: _dateDepart!,
        );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(capaciteProvider);
    final axesState = ref.watch(axesProvider);
    final vehiculeState = ref.watch(vehiculeProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Déclarer une capacité')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (state.derniereCapacite != null) _carteSucces(state.derniereCapacite!),
                if (state.erreur != null) _bandeauErreur(state.erreur!),
                const SizedBox(height: 8),

                _label('AXE'),
                const SizedBox(height: 8),
                DropdownButtonFormField<String>(
                  initialValue: _axeId,
                  decoration: _decoration(),
                  hint: Text(axesState.chargement ? 'Chargement…' : 'Choisir un axe'),
                  items: axesState.axes
                      .map((a) => DropdownMenuItem(value: a.id, child: Text('${a.origine} → ${a.destination}')))
                      .toList(),
                  onChanged: (v) => setState(() => _axeId = v),
                ),
                const SizedBox(height: 16),

                Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                  _label('VÉHICULE'),
                  TextButton(
                    onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const VehiculesScreen()))
                        .then((_) => ref.read(vehiculeProvider.notifier).chargerMesVehicules()),
                    child: const Text('Gérer ma flotte', style: TextStyle(fontSize: 12)),
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
                          vehiculeState.chargement ? 'Chargement…' : 'Aucun véhicule — ajoutez-en un via "Gérer ma flotte".',
                          style: const TextStyle(color: AppColors.texteMuet, fontSize: 13),
                        ),
                      )
                    : DropdownButtonFormField<String>(
                        initialValue: _vehiculeId,
                        decoration: _decoration(),
                        hint: const Text('Choisir un véhicule'),
                        items: vehiculeState.vehicules
                            .map((v) => DropdownMenuItem(value: v.id, child: Text(v.typeVehicule)))
                            .toList(),
                        onChanged: (v) => setState(() => _vehiculeId = v),
                      ),
                const SizedBox(height: 16),

                _label('POIDS DISPONIBLE (KG)'),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _poidsCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: _decoration(hint: 'Ex : 9500'),
                  validator: (v) {
                    if (v == null || v.isEmpty) return 'Champ obligatoire';
                    final n = double.tryParse(v);
                    if (n == null || n <= 0) return 'Nombre invalide';
                    return null;
                  },
                ),
                const SizedBox(height: 16),

                _label('DÉPART'),
                const SizedBox(height: 8),
                InkWell(
                  onTap: _choisirDate,
                  child: InputDecorator(
                    decoration: _decoration(),
                    child: Text(
                      _dateDepart == null
                          ? 'Choisir une date et une heure'
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
                        : const Text('Déclarer la capacité',
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
                  ),
                ),
                const SizedBox(height: 32),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _label(String text) => Text(text,
      style: const TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600));

  InputDecoration _decoration({String? hint}) => InputDecoration(
        hintText: hint,
        filled: true,
        fillColor: AppColors.surface,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.accent)),
      );

  Widget _carteSucces(CapaciteDeclaree capacite) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.succes.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.succes.withValues(alpha: 0.4)),
      ),
      child: Row(children: [
        const Icon(Icons.check_circle, color: AppColors.succes),
        const SizedBox(width: 10),
        Expanded(
          child: Text(
            capacite.publiee
                ? 'Capacité publiée — ${capacite.poidsTaxableKg.round()} kg taxables.'
                : 'Capacité enregistrée.',
            style: const TextStyle(color: AppColors.succes, fontSize: 13),
          ),
        ),
      ]),
    );
  }

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
