import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import '../providers/mission_provider.dart';
import '../providers/position_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/signature_pad.dart';
import '../widgets/top_notification.dart';
import 'plan_chargement_screen.dart';

// RG-070/EF-EXE-03 (audit CDC du 19 août) : ce que le formulaire de preuve
// (_FormulairePreuve, plus bas) doit renvoyer pour que _avancer() puisse
// appeler ajouterEtapeAvecPreuve.
class _Preuve {
  final List<Uint8List> photos;
  final Uint8List signature;
  const _Preuve({required this.photos, required this.signature});
}

const _categoriesIncident = [
  'Retard',
  'Marchandise endommagée',
  'Accident',
  'Panne véhicule',
  'Autre',
];

const _libellesStatut = {
  'EN_ATTENTE': 'En attente',
  'PRISE_EN_CHARGE': 'Prise en charge',
  'EN_TRANSIT': 'En transit',
  'LIVREE': 'Livrée',
  'ANNULEE': 'Annulée',
};

const _libellesEtape = {
  'PRISE_EN_CHARGE': 'Prise en charge',
  'EN_TRANSIT': 'En transit',
  'LIVRAISON': 'Livraison',
  'INCIDENT': 'Incident',
};

// S7 : détail + progression d'une mission (prise en charge → en transit →
// livraison, ou incident). Démarre/arrête automatiquement le suivi GPS (S6)
// selon le statut — c'est l'intégration prévue dès position_provider.dart.
class MissionDetailScreen extends ConsumerStatefulWidget {
  final Mission mission;
  const MissionDetailScreen({super.key, required this.mission});

  @override
  ConsumerState<MissionDetailScreen> createState() => _MissionDetailScreenState();
}

class _MissionDetailScreenState extends ConsumerState<MissionDetailScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(missionProvider.notifier).chargerDetail(widget.mission.missionId));
  }

  // RG-070/EF-EXE-03 : PRISE_EN_CHARGE/LIVRAISON exigent une preuve minimale
  // (photo(s) + signature tactile du tiers) — EN_TRANSIT n'est ni un
  // enlèvement ni une livraison, reste sur le chemin JSON sans preuve.
  bool _exigePreuve(String typeEtape) => typeEtape == 'PRISE_EN_CHARGE' || typeEtape == 'LIVRAISON';

  Future<void> _avancer(String typeEtape, String libelle) async {
    bool succes;
    if (_exigePreuve(typeEtape)) {
      final preuve = await showModalBottomSheet<_Preuve>(
        context: context,
        isScrollControlled: true,
        backgroundColor: AppColors.fond,
        shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
        builder: (_) => _FormulairePreuve(typeEtape: typeEtape),
      );
      if (preuve == null || !mounted) return;
      succes = await ref.read(missionProvider.notifier).ajouterEtapeAvecPreuve(
            widget.mission.missionId,
            typeEtape,
            libelle,
            preuve.photos,
            preuve.signature,
          );
    } else {
      succes = await ref.read(missionProvider.notifier).ajouterEtape(widget.mission.missionId, typeEtape, libelle);
    }
    if (!succes || !mounted) return;

    final positionNotifier = ref.read(positionProvider.notifier);
    if (typeEtape == 'PRISE_EN_CHARGE' || typeEtape == 'EN_TRANSIT') {
      positionNotifier.demarrerSuivi(widget.mission.missionId);
    } else if (typeEtape == 'LIVRAISON') {
      positionNotifier.arreterSuivi();
    }
  }

  // S19 (Sprint 19, "Back-office avancé, litiges") — le statut INCIDENT lui
  // même passe par le vrai endpoint (ajouterEtape, real S7). Seule
  // l'enrichissement (catégorie, description, photo) est composé côté app
  // et glissé dans le libellé — service-adm n'a aucun contrat pour ces
  // champs détaillés à ce jour (grille de décision, recours par opérateur
  // différent), donc pas de champ dédié côté backend pour l'instant.
  Future<void> _signalerIncident() async {
    final libelle = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.fond,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (_) => const _FormulaireIncident(),
    );
    if (libelle == null) return;
    await _avancer('INCIDENT', libelle);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(missionProvider);
    final detail = state.detail;
    final positionState = ref.watch(positionProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text('${widget.mission.origineNom ?? '—'} → ${widget.mission.destinationNom ?? '—'}')),
      body: state.chargement && detail == null
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (widget.mission.typeEmballageNom != null)
                    Container(
                      margin: const EdgeInsets.only(bottom: 16),
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceClaire,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Row(children: [
                        const Icon(Icons.inventory_2_outlined, color: AppColors.accent, size: 20),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            '${widget.mission.quantite ?? ''} × ${widget.mission.typeEmballageNom}'
                            '${widget.mission.poidsTaxableKg != null ? ' — ${widget.mission.poidsTaxableKg!.toStringAsFixed(0)} kg' : ''}',
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                          ),
                        ),
                      ]),
                    ),
                  if (widget.mission.destinataireNom != null)
                    Container(
                      margin: const EdgeInsets.only(bottom: 16),
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceClaire,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Row(children: [
                        const Icon(Icons.person_outline, color: AppColors.accent, size: 20),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Destinataire',
                                  style: TextStyle(fontSize: 11, color: AppColors.texteMuet, letterSpacing: 0.5)),
                              Text(
                                '${widget.mission.destinataireNom}'
                                '${widget.mission.destinataireTelephone != null ? ' · ${widget.mission.destinataireTelephone}' : ''}',
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                              ),
                            ],
                          ),
                        ),
                      ]),
                    ),
                  if (positionState.suiviActif)
                    Container(
                      margin: const EdgeInsets.only(bottom: 16),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.succes.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: AppColors.succes.withValues(alpha: 0.4)),
                      ),
                      child: const Row(children: [
                        Icon(Icons.gps_fixed, color: AppColors.succes, size: 18),
                        SizedBox(width: 8),
                        Text('Suivi GPS actif', style: TextStyle(color: AppColors.succes, fontSize: 13)),
                      ]),
                    ),
                  // BUG CORRIGE (retour utilisateur direct, 22 aout) : le
                  // badge "Suivi GPS actif" ci-dessus s'affiche des que
                  // demarrerSuivi() est appele, meme si l'envoi echoue
                  // ensuite (permission refusee, GPS coupe...) - aucune
                  // indication n'existait pour le chauffeur, qui croyait le
                  // suivi actif alors qu'aucune position ne partait jamais.
                  if (positionState.suiviActif && positionState.erreur != null)
                    Container(
                      margin: const EdgeInsets.only(bottom: 16),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.erreur.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
                      ),
                      child: Row(children: [
                        const Icon(Icons.gps_off, color: AppColors.erreur, size: 18),
                        const SizedBox(width: 8),
                        Expanded(child: Text(positionState.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 12))),
                      ]),
                    ),
                  if (state.erreur != null)
                    Container(
                      margin: const EdgeInsets.only(bottom: 16),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.erreur.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
                      ),
                      child: Row(children: [
                        const Icon(Icons.warning_amber, color: AppColors.erreur, size: 18),
                        const SizedBox(width: 8),
                        Expanded(child: Text(state.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 13))),
                      ]),
                    ),

                  Text('Statut : ${_libellesStatut[detail?.statut ?? widget.mission.statut] ?? widget.mission.statut}',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 20),

                  if (detail != null) _actions(detail, state.chargement),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    height: 44,
                    child: OutlinedButton.icon(
                      onPressed: () => Navigator.push(context,
                          MaterialPageRoute(builder: (_) => PlanChargementScreen(missionId: widget.mission.missionId))),
                      icon: const Icon(Icons.view_in_ar_outlined, size: 18),
                      label: const Text('Voir le plan de chargement'),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppColors.texte,
                        side: const BorderSide(color: AppColors.bordure),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),

                  Text('Chronologie', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 12),
                  if (detail == null || detail.etapes.isEmpty)
                    const Text('Aucune étape pour le moment.', style: TextStyle(color: AppColors.texteMuet))
                  else
                    ...detail.etapes.map(_carteEtape),
                ],
              ),
            ),
    );
  }

  Widget _actions(MissionDetail detail, bool chargement) {
    final Map<String, String> suivantes = switch (detail.statut) {
      'EN_ATTENTE' => {'PRISE_EN_CHARGE': 'Prise en charge'},
      'PRISE_EN_CHARGE' => {'EN_TRANSIT': 'En transit'},
      'EN_TRANSIT' => {'LIVRAISON': 'Confirmer la livraison'},
      _ => {},
    };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (final entry in suivantes.entries) ...[
          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton(
              onPressed: chargement ? null : () => _avancer(entry.key, entry.value),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: Text(entry.value, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
            ),
          ),
          const SizedBox(height: 10),
        ],
        if (detail.statut != 'LIVREE' && detail.statut != 'ANNULEE')
          SizedBox(
            width: double.infinity,
            height: 44,
            child: OutlinedButton(
              onPressed: chargement ? null : _signalerIncident,
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: AppColors.erreur),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: const Text('Signaler un incident', style: TextStyle(color: AppColors.erreur)),
            ),
          ),
      ],
    );
  }

  Widget _carteEtape(Etape etape) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Row(children: [
        Icon(etape.type == 'INCIDENT' ? Icons.warning_amber : Icons.check_circle,
            color: etape.type == 'INCIDENT' ? AppColors.erreur : AppColors.succes, size: 18),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(_libellesEtape[etape.type] ?? etape.type, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              Text(etape.libelle, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
            ],
          ),
        ),
      ]),
    );
  }
}

// S19 — formulaire de déclaration d'incident enrichie. La photo reste
// purement locale (aperçu, jamais envoyée) : aucun endpoint d'upload
// d'incident n'existe côté service-adm à ce jour — voir README.
class _FormulaireIncident extends StatefulWidget {
  const _FormulaireIncident();

  @override
  State<_FormulaireIncident> createState() => _FormulaireIncidentState();
}

class _FormulaireIncidentState extends State<_FormulaireIncident> {
  final _descriptionCtrl = TextEditingController();
  final _picker = ImagePicker();
  String _categorie = _categoriesIncident.first;
  File? _photo;

  @override
  void dispose() {
    _descriptionCtrl.dispose();
    super.dispose();
  }

  Future<void> _choisirPhoto() async {
    final image = await _picker.pickImage(source: ImageSource.camera, imageQuality: 80, maxWidth: 1600);
    if (image == null) return;
    setState(() => _photo = File(image.path));
  }

  void _valider() {
    final description = _descriptionCtrl.text.trim();
    final libelle = description.isEmpty ? '[$_categorie]' : '[$_categorie] $description';
    Navigator.pop(context, libelle);
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(context).viewInsets.bottom + 20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Signaler un incident', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 4),
          const Text(
            'Grille de décision et recours traités par le Bureau — pas encore automatisés côté app.',
            style: TextStyle(color: AppColors.texteMuet, fontSize: 11.5),
          ),
          const SizedBox(height: 20),
          const Text('CATÉGORIE', style: TextStyle(fontSize: 11, letterSpacing: 1.1,
              color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            initialValue: _categorie,
            decoration: InputDecoration(
              filled: true,
              fillColor: AppColors.surface,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
            ),
            items: _categoriesIncident.map((c) => DropdownMenuItem(value: c, child: Text(c))).toList(),
            onChanged: (v) => setState(() => _categorie = v!),
          ),
          const SizedBox(height: 16),
          const Text('DESCRIPTION', style: TextStyle(fontSize: 11, letterSpacing: 1.1,
              color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          TextField(
            controller: _descriptionCtrl,
            maxLines: 3,
            decoration: InputDecoration(
              hintText: 'Détaillez ce qui s\'est passé (optionnel)',
              filled: true,
              fillColor: AppColors.surface,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
            ),
          ),
          const SizedBox(height: 16),
          Row(children: [
            OutlinedButton.icon(
              onPressed: _choisirPhoto,
              icon: const Icon(Icons.camera_alt_outlined, size: 18),
              label: Text(_photo == null ? 'Ajouter une photo (optionnel)' : 'Photo jointe'),
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.texte,
                side: const BorderSide(color: AppColors.bordure),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
            ),
            if (_photo != null) ...[
              const SizedBox(width: 8),
              const Icon(Icons.check_circle, color: AppColors.succes, size: 18),
            ],
          ]),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton(
              onPressed: _valider,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: const Text('Envoyer le signalement', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
            ),
          ),
        ],
      ),
    );
  }
}

// RG-070/EF-EXE-03 (audit CDC du 19 août, bloquant §1.3 "livraison sans
// preuve libère le séquestre") : au moins une photo + une signature tactile
// du tiers, obligatoires pour PRISE_EN_CHARGE et LIVRAISON (backend rejette
// désormais ces deux étapes sans preuve — PREUVE_MANQUANTE). Le second mode
// de validation tiers prévu par le CDC (code SMS) reste hors périmètre côté
// backend (destinataireTelephone non propagé jusqu'à service-exe) — signature
// uniquement pour l'instant.
class _FormulairePreuve extends StatefulWidget {
  final String typeEtape;
  const _FormulairePreuve({required this.typeEtape});

  @override
  State<_FormulairePreuve> createState() => _FormulairePreuveState();
}

class _FormulairePreuveState extends State<_FormulairePreuve> {
  final _picker = ImagePicker();
  final _photos = <File>[];
  final _signatureKey = GlobalKey<SignaturePadState>();
  bool _envoiEnCours = false;

  Future<void> _ajouterPhoto() async {
    if (_photos.length >= 3) return;
    final image = await _picker.pickImage(source: ImageSource.camera, imageQuality: 80, maxWidth: 1600);
    if (image == null) return;
    setState(() => _photos.add(File(image.path)));
  }

  Future<void> _valider() async {
    final padState = _signatureKey.currentState;
    if (_photos.isEmpty || padState == null || padState.estVide) {
      afficherNotification(context, message: 'Une photo et une signature sont obligatoires.', couleur: AppColors.erreur, icone: Icons.error_outline);
      return;
    }
    setState(() => _envoiEnCours = true);
    final signaturePng = await padState.capturerPng();
    if (signaturePng == null) {
      setState(() => _envoiEnCours = false);
      return;
    }
    final photosBytes = await Future.wait(_photos.map((f) => f.readAsBytes()));
    if (!mounted) return;
    Navigator.pop(context, _Preuve(photos: photosBytes, signature: signaturePng));
  }

  @override
  Widget build(BuildContext context) {
    final titre = widget.typeEtape == 'PRISE_EN_CHARGE' ? 'Preuve de prise en charge' : 'Preuve de livraison';
    return Padding(
      padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(context).viewInsets.bottom + 20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(titre, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 4),
          const Text(
            'Photo(s) de la marchandise + signature du destinataire — obligatoire (RG-070).',
            style: TextStyle(color: AppColors.texteMuet, fontSize: 11.5),
          ),
          const SizedBox(height: 20),
          const Text('PHOTOS (au moins 1)', style: TextStyle(fontSize: 11, letterSpacing: 1.1,
              color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final photo in _photos)
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.file(photo, width: 72, height: 72, fit: BoxFit.cover),
                ),
              if (_photos.length < 3)
                InkWell(
                  onTap: _ajouterPhoto,
                  child: Container(
                    width: 72,
                    height: 72,
                    decoration: BoxDecoration(
                      color: AppColors.surface,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: AppColors.bordure),
                    ),
                    child: const Icon(Icons.camera_alt_outlined, color: AppColors.texteMuet),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 20),
          const Text('SIGNATURE DU DESTINATAIRE', style: TextStyle(fontSize: 11, letterSpacing: 1.1,
              color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          SignaturePad(key: _signatureKey),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: () => setState(() => _signatureKey.currentState?.effacer()),
              child: const Text('Effacer'),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton(
              onPressed: _envoiEnCours ? null : _valider,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: _envoiEnCours
                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Valider', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
            ),
          ),
        ],
      ),
    );
  }
}
