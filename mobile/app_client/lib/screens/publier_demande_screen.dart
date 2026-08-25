import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl_phone_field/intl_phone_field.dart';
import '../l10n/app_localizations.dart';
import '../providers/demande_provider.dart';
import '../providers/axes_provider.dart';
import '../models/catalogue_emballage_model.dart';
import '../models/demande_model.dart';
import '../theme/app_theme.dart';
import '../widgets/top_notification.dart';

class PublierDemandeScreen extends ConsumerStatefulWidget {
  // CRUD demande (audit de suivi Mobile) : pas de vrai endpoint de
  // modification côté backend (changer le poids/l'axe demanderait de
  // rerésoudre l'axe et recalculer les coordonnées) — "modifier" republie
  // une nouvelle demande pré-remplie puis annule l'ancienne, en réutilisant
  // le flux publier()/annuler() déjà validé plutôt qu'un PATCH risqué.
  final DemandeModel? demandeAModifier;
  const PublierDemandeScreen({super.key, this.demandeAModifier});

  @override
  ConsumerState<PublierDemandeScreen> createState() => _PublierDemandeScreenState();
}

class _PublierDemandeScreenState extends ConsumerState<PublierDemandeScreen> {
  final _formKey = GlobalKey<FormState>();
  final _villeDepartCtrl = TextEditingController();
  final _villeArriveeCtrl = TextEditingController();
  final _quantiteCtrl = TextEditingController(text: '1');
  final _destinataireNomCtrl = TextEditingController();
  String _destinataireTelephone = '';

  CatalogueEmballageModel? _emballageSelectionne;
  String? _axeSelectionneId;
  bool _fragile = false, _perissable = false, _dangereuse = false, _grandeValeur = false;
  String _typeDisponibilite = 'DES_QUE_POSSIBLE';
  String _modeCollecte = 'DOMICILE';

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(axesProvider.notifier).charger();
      _preRemplirSiModification();
    });
  }

  void _preRemplirSiModification() {
    final d = widget.demandeAModifier;
    if (d == null) return;
    _villeDepartCtrl.text = d.villeDepart;
    _villeArriveeCtrl.text = d.villeArrivee;
    _quantiteCtrl.text = '${d.quantite}';
    _destinataireNomCtrl.text = d.destinataireNom;
    _destinataireTelephone = d.destinataireTelephone;
    _fragile = d.fragile;
    _perissable = d.perissable;
    _dangereuse = d.dangereuse;
    _grandeValeur = d.grandeValeur;
    _typeDisponibilite = d.typeDisponibilite;
    _modeCollecte = d.modeCollecte;
    // Le catalogue (types d'emballage) est chargé globalement par
    // demandeProvider au démarrage de l'app — déjà disponible ici dans la
    // quasi-totalité des cas (l'utilisateur vient de "Mes demandes", donc
    // le catalogue a déjà servi à publier la demande d'origine).
    final catalogue = ref.read(demandeProvider).catalogue;
    for (final e in catalogue) {
      if (e.nom == d.typeEmballageNom) {
        setState(() => _emballageSelectionne = e);
        break;
      }
    }
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _villeDepartCtrl.dispose(); _villeArriveeCtrl.dispose();
    _quantiteCtrl.dispose(); _destinataireNomCtrl.dispose();
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

  void _changerQuantite(int delta) {
    final q = int.tryParse(_quantiteCtrl.text) ?? 1;
    final nouvelle = (q + delta).clamp(1, 9999);
    setState(() => _quantiteCtrl.text = '$nouvelle');
  }

  // Donne un repère concret (type de véhicule) au client plutôt qu'un
  // chiffre brut en kg qu'il ne sait pas interpréter (ex : "2500 kg" seul).
  String _vehiculeSuggere(AppLocalizations t, double poidsKg) {
    if (poidsKg <= 500) return t.vehiculeCamionnette;
    if (poidsKg <= 1500) return t.vehiculeFourgon;
    if (poidsKg <= 3500) return t.vehiculeCamionLeger;
    if (poidsKg <= 8000) return t.vehiculeCamionMoyen;
    if (poidsKg <= 20000) return t.vehiculeCamionLourd;
    return t.vehiculeSemiRemorque;
  }

  Future<void> _publier() async {
    final t = AppLocalizations.of(context);
    if (!_formKey.currentState!.validate()) return;
    if (_emballageSelectionne == null) {
      afficherNotification(context, message: t.choisirTypeMarchandise, couleur: AppColors.erreur, icone: Icons.error_outline);
      return;
    }
    if (_destinataireTelephone.isEmpty) {
      afficherNotification(context, message: t.telephoneRenseigner, couleur: AppColors.erreur, icone: Icons.error_outline);
      return;
    }

    if (widget.demandeAModifier == null) {
      afficherNotification(
        context,
        message: t.prixEstimationMessage,
        duree: const Duration(seconds: 4),
      );
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
      destinataireTelephone: _destinataireTelephone,
    );

    if (!succes || !mounted) return;

    final ancienne = widget.demandeAModifier;
    if (ancienne != null) {
      // La nouvelle demande est déjà publiée à ce stade (succes==true) —
      // annuler l'ancienne est un nettoyage, pas une condition de succès :
      // ne pas bloquer l'utilisateur si ça échoue, juste l'avertir.
      final erreurAnnulation = await ref.read(demandeProvider.notifier).annulerDemande(ancienne.id);
      if (!mounted) return;
      if (erreurAnnulation != null) {
        afficherNotification(
          context,
          message: t.nouvelleDemandeAnnulationEchouee(erreurAnnulation),
          couleur: AppColors.erreur,
          icone: Icons.error_outline,
        );
      } else {
        afficherNotification(context, message: t.demandeModifiee, couleur: AppColors.succes, icone: Icons.check_circle);
      }
    }

    if (mounted) Navigator.pop(context);
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

  Widget _stepperQuantite(AppLocalizations t) {
    final q = int.tryParse(_quantiteCtrl.text) ?? 1;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Row(
        children: [
          IconButton(
            onPressed: () => _changerQuantite(-1),
            icon: const Icon(Icons.remove_circle_outline, color: AppColors.accent),
          ),
          Expanded(
            child: Text('$q', textAlign: TextAlign.center,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          ),
          IconButton(
            onPressed: () => _changerQuantite(1),
            icon: const Icon(Icons.add_circle_outline, color: AppColors.accent),
          ),
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: Text(t.unites, style: const TextStyle(color: AppColors.texteMuet, fontSize: 13)),
          ),
        ],
      ),
    );
  }

  Widget _label(String text) => Padding(
        padding: const EdgeInsets.only(bottom: 8, top: 16),
        child: Text(text, style: const TextStyle(fontSize: 11, letterSpacing: 1.1,
            color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
      );

  // Regroupe les champs d'une même étape dans une carte distincte (au lieu
  // d'un long formulaire à plat) — plus facile à parcourir visuellement.
  Widget _section({required IconData icone, required String titre, required List<Widget> enfants}) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            Container(
              width: 32, height: 32,
              decoration: BoxDecoration(color: AppColors.surfaceClaire, borderRadius: BorderRadius.circular(8)),
              child: Icon(icone, color: AppColors.accent, size: 17),
            ),
            const SizedBox(width: 10),
            Text(titre, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
          ]),
          ...enfants,
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final demandeState = ref.watch(demandeProvider);
    final axesState = ref.watch(axesProvider);
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(widget.demandeAModifier == null ? t.envoyerMarchandise : t.modifierLaDemande)),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _section(icone: Icons.map_outlined, titre: t.sectionLieu, enfants: [
              // S15 — sélecteur d'axe (GET /api/geo/axes?tenantId=...),
              // remplit les villes ci-dessous mais reste facultatif — la
              // saisie libre fonctionne toujours (ex. axe non couvert).
              if (axesState.axes.length > 1) ...[
                _label(t.axeFacultatif),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: axesState.axes.map((axe) {
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
              _label(t.champVilleDepart),
              TextFormField(
                controller: _villeDepartCtrl,
                decoration: _decoration(t.hintVilleDepart, Icons.trip_origin),
                validator: (v) => v!.isEmpty ? t.obligatoire : null,
              ),
              _label(t.champVilleArrivee),
              TextFormField(
                controller: _villeArriveeCtrl,
                decoration: _decoration(t.hintVilleArrivee, Icons.place),
                validator: (v) => v!.isEmpty ? t.obligatoire : null,
              ),
              ]),

              _section(icone: Icons.inventory_2_outlined, titre: t.sectionMarchandise, enfants: [
              _label(t.typeMarchandise),
              if (demandeState.catalogue.isEmpty)
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceClaire,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.wifi_off, color: AppColors.texteMuet, size: 18),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(t.catalogueIndisponible,
                            style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                      ),
                      TextButton(
                        onPressed: () => ref.read(demandeProvider.notifier).chargerCatalogue(),
                        child: Text(t.reessayer),
                      ),
                    ],
                  ),
                )
              else
              DropdownButtonFormField<CatalogueEmballageModel>(
                initialValue: _emballageSelectionne,
                decoration: _decoration(t.selectionnerLeType),
                hint: Text(t.selectionnerLeType),
                items: demandeState.catalogue
                    .map((e) => DropdownMenuItem(value: e, child: Text(e.nom)))
                    .toList(),
                onChanged: (e) => setState(() => _emballageSelectionne = e),
                validator: (v) => v == null ? t.choisirTypeMarchandise : null,
              ),

              _label(_emballageSelectionne == null
                  ? t.quantiteNombreUnites
                  : t.quantiteNombreDe(_emballageSelectionne!.nom.toUpperCase())),
              _stepperQuantite(t),

              // Champ caché : conserve le comportement de validation existant
              // (obligatoire, entier positif) sans dupliquer la logique.
              Offstage(
                offstage: true,
                child: TextFormField(
                  controller: _quantiteCtrl,
                  validator: (v) {
                    if (v == null || v.isEmpty) return t.obligatoire;
                    if (int.tryParse(v) == null || int.parse(v) <= 0) return t.nombreInvalide;
                    return null;
                  },
                ),
              ),

              if (_emballageSelectionne != null) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceClaire,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${_quantiteCtrl.text.isEmpty ? 0 : _quantiteCtrl.text} × ${_emballageSelectionne!.nom} '
                        '(${_emballageSelectionne!.poidsUnitaireKg.toStringAsFixed(0)} kg/unité)',
                        style: const TextStyle(fontSize: 12, color: AppColors.texteMuet, fontWeight: FontWeight.w600),
                      ),
                      const SizedBox(height: 10),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          _ChiffreCalcule(t.poidsTotalLabel, '${_poidsTotal.toStringAsFixed(0)} kg'),
                          _ChiffreCalcule(t.volumeTotalLabel, '${_volumeTotal.toStringAsFixed(2)} m³'),
                        ],
                      ),
                      const Divider(height: 20),
                      Row(
                        children: [
                          const Icon(Icons.local_shipping_outlined, size: 16, color: AppColors.accent),
                          const SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              t.vehiculeAdapte(_vehiculeSuggere(t, _poidsTotal)),
                              style: const TextStyle(fontSize: 12, color: AppColors.texte, fontWeight: FontWeight.w600),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ],

              // ── Nature particulière ────────────────────────
              _label(t.naturesParticulieres),
              Wrap(spacing: 8, runSpacing: 8, children: [
                _Commutateur(t.fragile, _fragile, (v) => setState(() => _fragile = v)),
                _Commutateur(t.perissable, _perissable, (v) => setState(() => _perissable = v)),
                _Commutateur(t.dangereuse, _dangereuse, (v) => setState(() => _dangereuse = v)),
                _Commutateur(t.grandeValeur, _grandeValeur, (v) => setState(() => _grandeValeur = v)),
              ]),
              ]),

              _section(icone: Icons.schedule_outlined, titre: t.sectionModalites, enfants: [
              _label(t.disponibiliteLabel),
              DropdownButtonFormField<String>(
                initialValue: _typeDisponibilite,
                decoration: _decoration(''),
                items: [
                  DropdownMenuItem(value: 'DES_QUE_POSSIBLE', child: Text(t.desQuePossible)),
                  DropdownMenuItem(value: 'DATE_PRECISE', child: Text(t.datePrecise)),
                  DropdownMenuItem(value: 'PLAGE', child: Text(t.dansUnePlage)),
                ],
                onChanged: (v) => setState(() => _typeDisponibilite = v!),
              ),

              // ── Comment ─────────────────────────────────
              _label(t.modeCollecteLabel),
              DropdownButtonFormField<String>(
                initialValue: _modeCollecte,
                decoration: _decoration(''),
                items: [
                  DropdownMenuItem(value: 'DOMICILE', child: Text(t.aDomicile)),
                  DropdownMenuItem(value: 'POINT_RELAIS', child: Text(t.pointRelais)),
                ],
                onChanged: (v) => setState(() => _modeCollecte = v!),
              ),
              ]),

              _section(icone: Icons.person_pin_circle_outlined, titre: t.sectionDestinataire, enfants: [
              _label(t.labelNom),
              TextFormField(
                controller: _destinataireNomCtrl,
                decoration: _decoration(t.hintDestinataireNom, Icons.person),
                validator: (v) => v!.isEmpty ? t.obligatoire : null,
              ),
              _label(t.champTelephone),
              IntlPhoneField(
                initialCountryCode: 'CM',
                disableLengthCheck: true,
                decoration: _decoration('', Icons.phone).copyWith(hintText: null),
                dropdownTextStyle: const TextStyle(color: AppColors.texte),
                onChanged: (phone) => _destinataireTelephone = phone.completeNumber,
              ),
              ]),

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
                      : Text(widget.demandeAModifier == null ? t.publierLaDemande : t.enregistrerModifications,
                          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
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
