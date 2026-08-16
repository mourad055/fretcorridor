import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/demande_provider.dart';
import '../models/catalogue_emballage_model.dart';
import '../mock/axe_mock.dart';
import '../theme/app_theme.dart';

class PublierDemandeScreen extends ConsumerStatefulWidget {
  const PublierDemandeScreen({super.key});

  @override
  ConsumerState<PublierDemandeScreen> createState() => _PublierDemandeScreenState();
}

class _PublierDemandeScreenState extends ConsumerState<PublierDemandeScreen> {
  final _formKey = GlobalKey<FormState>();
  final _villeDepartCtrl = TextEditingController();
  final _villeArriveeCtrl = TextEditingController();
  final _quantiteCtrl = TextEditingController(text: '1');
  final _destinataireNomCtrl = TextEditingController();
  final _destinataireTelCtrl = TextEditingController();

  CatalogueEmballageModel? _emballageSelectionne;
  String? _axeSelectionneId;
  bool _fragile = false, _perissable = false, _dangereuse = false, _grandeValeur = false;
  String _typeDisponibilite = 'DES_QUE_POSSIBLE';
  String _modeCollecte = 'DOMICILE';

  @override
  void dispose() {
    _villeDepartCtrl.dispose(); _villeArriveeCtrl.dispose();
    _quantiteCtrl.dispose(); _destinataireNomCtrl.dispose(); _destinataireTelCtrl.dispose();
    super.dispose();
  }

  double get _poidsTotal {
    if (_emballageSelectionne == null) return 0;
    final q = int.tryParse(_quantiteCtrl.text) ?? 0;
    return _emballageSelectionne!.poidsUnitaireKg * q;
  }

  double get _volumeTotal {
    if (_emballageSelectionne == null) return 0;
    final q = int.tryParse(_quantiteCtrl.text) ?? 0;
    return _emballageSelectionne!.volumeUnitaireM3 * q;
  }

  Future<void> _publier() async {
    if (!_formKey.currentState!.validate()) return;
    if (_emballageSelectionne == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Choisissez un type de marchandise'), backgroundColor: AppColors.erreur),
      );
      return;
    }

    final succes = await ref.read(demandeProvider.notifier).publier(
      villeDepart: _villeDepartCtrl.text.trim(),
      villeArrivee: _villeArriveeCtrl.text.trim(),
      typeEmballageId: _emballageSelectionne!.id,
      quantite: int.parse(_quantiteCtrl.text),
      fragile: _fragile,
      perissable: _perissable,
      dangereuse: _dangereuse,
      grandeValeur: _grandeValeur,
      typeDisponibilite: _typeDisponibilite,
      modeCollecte: _modeCollecte,
      destinataireNom: _destinataireNomCtrl.text.trim(),
      destinataireTelephone: _destinataireTelCtrl.text.trim(),
    );

    if (succes && mounted) Navigator.pop(context);
  }

  InputDecoration _decoration(String hint, [IconData? icon]) {
    return InputDecoration(
      hintText: hint,
      filled: true,
      fillColor: AppColors.surface,
      prefixIcon: icon != null ? Icon(icon, color: AppColors.texteMuet, size: 20) : null,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
      enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
      focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.accent)),
    );
  }

  Widget _label(String text) => Padding(
        padding: const EdgeInsets.only(bottom: 8, top: 16),
        child: Text(text, style: const TextStyle(fontSize: 11, letterSpacing: 1.1,
            color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
      );

  @override
  Widget build(BuildContext context) {
    final demandeState = ref.watch(demandeProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Envoyer une marchandise')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // ── Où ──────────────────────────────────────
              Text('Où', style: Theme.of(context).textTheme.titleMedium),
              // S15 — MOCK (axe_mock.dart) : sélecteur d'axe, remplit les
              // villes ci-dessous mais reste facultatif — la saisie libre
              // fonctionne toujours (ex. axe non couvert par la démo).
              if (axesMockDisponibles.length > 1) ...[
                _label('AXE (FACULTATIF)'),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: axesMockDisponibles.map((axe) {
                    final selectionne = _axeSelectionneId == axe.id;
                    return GestureDetector(
                      onTap: () => setState(() {
                        _axeSelectionneId = axe.id;
                        _villeDepartCtrl.text = axe.origine;
                        _villeArriveeCtrl.text = axe.destination;
                      }),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                        decoration: BoxDecoration(
                          color: selectionne ? AppColors.accent : AppColors.surface,
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: selectionne ? AppColors.accent : AppColors.bordure),
                        ),
                        child: Text('${axe.origine} → ${axe.destination}',
                            style: TextStyle(color: selectionne ? Colors.white : AppColors.texte, fontSize: 13)),
                      ),
                    );
                  }).toList(),
                ),
              ],
              _label('VILLE DE DÉPART'),
              TextFormField(
                controller: _villeDepartCtrl,
                decoration: _decoration('Ex : Yaoundé', Icons.trip_origin),
                validator: (v) => v!.isEmpty ? 'Obligatoire' : null,
              ),
              _label('VILLE D\'ARRIVÉE'),
              TextFormField(
                controller: _villeArriveeCtrl,
                decoration: _decoration('Ex : Douala', Icons.place),
                validator: (v) => v!.isEmpty ? 'Obligatoire' : null,
              ),

              // ── Quoi / Combien ────────────────────────────
              const SizedBox(height: 8),
              Text('Quoi', style: Theme.of(context).textTheme.titleMedium),
              _label('TYPE DE MARCHANDISE'),
              Wrap(
                spacing: 8, runSpacing: 8,
                children: demandeState.catalogue.map((e) {
                  final selectionne = _emballageSelectionne?.id == e.id;
                  return GestureDetector(
                    onTap: () => setState(() => _emballageSelectionne = e),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      decoration: BoxDecoration(
                        color: selectionne ? AppColors.accent : AppColors.surface,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(color: selectionne ? AppColors.accent : AppColors.bordure),
                      ),
                      child: Text(e.nom, style: TextStyle(
                        color: selectionne ? Colors.white : AppColors.texte, fontSize: 13,
                      )),
                    ),
                  );
                }).toList(),
              ),

              _label('QUANTITÉ'),
              TextFormField(
                controller: _quantiteCtrl,
                keyboardType: TextInputType.number,
                decoration: _decoration('Ex : 10', Icons.numbers),
                onChanged: (_) => setState(() {}),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'Obligatoire';
                  if (int.tryParse(v) == null || int.parse(v) <= 0) return 'Nombre invalide';
                  return null;
                },
              ),

              if (_emballageSelectionne != null) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceClaire,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: [
                      _ChiffreCalcule('Poids total', '${_poidsTotal.toStringAsFixed(0)} kg'),
                      _ChiffreCalcule('Volume total', '${_volumeTotal.toStringAsFixed(2)} m³'),
                    ],
                  ),
                ),
              ],

              // ── Nature particulière ────────────────────────
              _label('NATURE PARTICULIÈRE'),
              Wrap(spacing: 8, runSpacing: 8, children: [
                _Commutateur('Fragile', _fragile, (v) => setState(() => _fragile = v)),
                _Commutateur('Périssable', _perissable, (v) => setState(() => _perissable = v)),
                _Commutateur('Dangereuse', _dangereuse, (v) => setState(() => _dangereuse = v)),
                _Commutateur('Grande valeur', _grandeValeur, (v) => setState(() => _grandeValeur = v)),
              ]),

              // ── Quand ───────────────────────────────────
              _label('DISPONIBILITÉ'),
              DropdownButtonFormField<String>(
                initialValue: _typeDisponibilite,
                decoration: _decoration(''),
                items: const [
                  DropdownMenuItem(value: 'DES_QUE_POSSIBLE', child: Text('Dès que possible')),
                  DropdownMenuItem(value: 'DATE_PRECISE', child: Text('Date précise')),
                  DropdownMenuItem(value: 'PLAGE', child: Text('Dans une plage')),
                ],
                onChanged: (v) => setState(() => _typeDisponibilite = v!),
              ),

              // ── Comment ─────────────────────────────────
              _label('MODE DE COLLECTE'),
              DropdownButtonFormField<String>(
                initialValue: _modeCollecte,
                decoration: _decoration(''),
                items: const [
                  DropdownMenuItem(value: 'DOMICILE', child: Text('À domicile')),
                  DropdownMenuItem(value: 'POINT_RELAIS', child: Text('Point relais')),
                ],
                onChanged: (v) => setState(() => _modeCollecte = v!),
              ),

              // ── Qui reçoit ──────────────────────────────
              const SizedBox(height: 8),
              Text('Destinataire', style: Theme.of(context).textTheme.titleMedium),
              _label('NOM'),
              TextFormField(
                controller: _destinataireNomCtrl,
                decoration: _decoration('Ex : Paul Nkomo', Icons.person),
                validator: (v) => v!.isEmpty ? 'Obligatoire' : null,
              ),
              _label('TÉLÉPHONE'),
              TextFormField(
                controller: _destinataireTelCtrl,
                keyboardType: TextInputType.phone,
                decoration: _decoration('+237 6XX XXX XXX', Icons.phone),
                validator: (v) => v!.isEmpty ? 'Obligatoire' : null,
              ),

              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.surfaceClaire,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Text(
                  'Le prix affiché après publication est une estimation. Le prix ferme sera connu à l\'acceptation d\'une proposition.',
                  style: TextStyle(color: AppColors.texteMuet, fontSize: 11.5),
                ),
              ),

              if (demandeState.erreur != null) ...[
                const SizedBox(height: 12),
                Text(demandeState.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 12)),
              ],

              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                height: 54,
                child: ElevatedButton(
                  onPressed: demandeState.publicationEnCours ? null : _publier,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.accent,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  ),
                  child: demandeState.publicationEnCours
                      ? const SizedBox(height: 22, width: 22,
                          child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                      : const Text('Publier la demande', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
                ),
              ),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }
}

class _ChiffreCalcule extends StatelessWidget {
  final String label;
  final String valeur;
  const _ChiffreCalcule(this.label, this.valeur);

  @override
  Widget build(BuildContext context) {
    return Column(children: [
      Text(valeur, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: AppColors.accent)),
      Text(label, style: const TextStyle(fontSize: 11, color: AppColors.texteMuet)),
    ]);
  }
}

class _Commutateur extends StatelessWidget {
  final String label;
  final bool valeur;
  final ValueChanged<bool> onChanged;
  const _Commutateur(this.label, this.valeur, this.onChanged);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => onChanged(!valeur),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: valeur ? AppColors.accent : AppColors.surface,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: valeur ? AppColors.accent : AppColors.bordure),
        ),
        child: Text(label, style: TextStyle(color: valeur ? Colors.white : AppColors.texteMuet, fontSize: 12)),
      ),
    );
  }
}
