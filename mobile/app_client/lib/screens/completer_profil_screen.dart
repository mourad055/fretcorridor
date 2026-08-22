import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:image_picker/image_picker.dart';
import '../providers/auth_provider.dart';
import '../widgets/top_notification.dart';
import '../providers/kyc_provider.dart';
import '../theme/app_theme.dart';

enum _TypeProfil { particulier, entreprise }

// RG-011 : niveau 1 = identité déclarée ET au moins une pièce déposée, dans
// n'importe quel ordre — d'où les deux étapes indépendantes de cet écran
// (avant, la complétion s'arrêtait après l'étape 1 et fermait l'écran en
// laissant croire le profil complet, alors que "publier une demande" reste
// bloqué tant que l'étape 2 n'est pas faite).
class CompleterProfilScreen extends ConsumerStatefulWidget {
  const CompleterProfilScreen({super.key});

  @override
  ConsumerState<CompleterProfilScreen> createState() => _CompleterProfilScreenState();
}

class _CompleterProfilScreenState extends ConsumerState<CompleterProfilScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nomCtrl = TextEditingController();
  final _prenomCtrl = TextEditingController();
  final _raisonSocialeCtrl = TextEditingController();
  final _rccmCtrl = TextEditingController();
  final _picker = ImagePicker();
  _TypeProfil _type = _TypeProfil.particulier;
  bool _modeEdition = false;
  bool _verificationEnCours = false;

  @override
  void dispose() {
    _nomCtrl.dispose();
    _prenomCtrl.dispose();
    _raisonSocialeCtrl.dispose();
    _rccmCtrl.dispose();
    super.dispose();
  }

  Future<void> _enregistrerIdentite() async {
    if (!_formKey.currentState!.validate()) return;
    final notifier = ref.read(kycProvider.notifier);
    final succes = _type == _TypeProfil.particulier
        ? await notifier.completerParticulier(nom: _nomCtrl.text.trim(), prenom: _prenomCtrl.text.trim())
        : await notifier.completerEntreprise(
            raisonSociale: _raisonSocialeCtrl.text.trim(),
            numeroRegistreCommerce: _rccmCtrl.text.trim().isEmpty ? null : _rccmCtrl.text.trim(),
          );
    if (succes && mounted) setState(() => _modeEdition = false);
  }

  void _ouvrirEdition(KycState kycState) {
    _type = kycState.type == 'ENTREPRISE' ? _TypeProfil.entreprise : _TypeProfil.particulier;
    _nomCtrl.text = kycState.nom ?? '';
    _prenomCtrl.text = kycState.prenom ?? '';
    _raisonSocialeCtrl.text = kycState.raisonSociale ?? '';
    setState(() => _modeEdition = true);
  }

  Future<void> _deposerPiece() async {
    final image = await _picker.pickImage(source: ImageSource.camera, imageQuality: 80, maxWidth: 1600);
    if (image == null) return;

    setState(() => _verificationEnCours = true);
    final estUnePiece = await _ressemblePieceIdentite(image.path);
    if (mounted) setState(() => _verificationEnCours = false);

    if (!estUnePiece) {
      if (mounted) {
        afficherNotification(
          context,
          message: 'Cette photo ne ressemble pas à une pièce d\'identité — cadrez bien le document (texte lisible) et réessayez.',
          couleur: AppColors.erreur,
          icone: Icons.error_outline,
        );
      }
      return;
    }

    await ref.read(kycProvider.notifier).deposerDocument('CNI', File(image.path));
  }

  // Vérification légère côté appareil : une vraie pièce d'identité contient
  // toujours BEAUCOUP de texte imprimé sur plusieurs lignes (nom, prénom,
  // numéro, dates de naissance/délivrance...) — un simple seuil de longueur
  // de texte (12 caractères) laissait passer des photos quelconques (un
  // panneau, un objet avec une étiquette) qui contiennent un peu de texte
  // parasite. Triple condition plus stricte : assez de texte au total, sur
  // plusieurs lignes distinctes, avec au moins quelques chiffres (numéro de
  // pièce / dates, quasi systématiques sur une CNI/passeport). Pas une
  // lecture fiable à 100% du contenu, mais rejette nettement mieux les
  // photos manifestement hors sujet, sans dépendre d'un service externe.
  Future<bool> _ressemblePieceIdentite(String chemin) async {
    final recognizer = TextRecognizer(script: TextRecognitionScript.latin);
    try {
      final resultat = await recognizer.processImage(InputImage.fromFilePath(chemin));
      final texte = resultat.text;
      final texteUtile = texte.replaceAll(RegExp(r'\s'), '');
      final lignes = texte.split('\n').where((l) => l.trim().length >= 2).length;
      final chiffres = RegExp(r'\d').allMatches(texte).length;
      return texteUtile.length >= 25 && lignes >= 3 && chiffres >= 2;
    } catch (_) {
      // Échec technique de l'OCR : par sécurité, on refuse plutôt que
      // d'accepter à l'aveugle (le dépôt d'une pièce non vérifiée est plus
      // grave qu'un dépôt légitime à refaire).
      return false;
    } finally {
      await recognizer.close();
    }
  }

  InputDecoration _decoration(String hint, IconData icon) {
    return InputDecoration(
      hintText: hint,
      filled: true,
      fillColor: AppColors.surface,
      prefixIcon: Icon(icon, color: AppColors.texteMuet, size: 20),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
      enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
      focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.accent)),
    );
  }

  Widget _label(String text) => Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Text(text, style: const TextStyle(fontSize: 11, letterSpacing: 1.1,
            color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
      );

  @override
  Widget build(BuildContext context) {
    final kycState = ref.watch(kycProvider);
    final estComplet = kycState.niveauKyc != 'NIVEAU_0';

    return Scaffold(
      backgroundColor: AppColors.fond,
      body: Column(
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 28),
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [AppColors.accent, AppColors.accentProfond],
              ),
              borderRadius: BorderRadius.vertical(bottom: Radius.circular(28)),
            ),
            child: SafeArea(
              bottom: false,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    IconButton(
                      icon: const Icon(Icons.arrow_back, color: Colors.white),
                      onPressed: () => _modeEdition ? setState(() => _modeEdition = false) : Navigator.pop(context),
                    ),
                  ]),
                  const SizedBox(height: 4),
                  Row(children: [
                    Container(
                      width: 56, height: 56,
                      decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.15), shape: BoxShape.circle),
                      child: const Icon(Icons.person, color: Colors.white, size: 30),
                    ),
                    const SizedBox(width: 14),
                    Text('Mon profil', style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: Colors.white)),
                  ]),
                ],
              ),
            ),
          ),
          Expanded(
            child: estComplet && !_modeEdition ? _profilComplete(kycState) : _formulaireOuChecklist(kycState, estComplet),
          ),
        ],
      ),
    );
  }

  Widget _formulaireOuChecklist(KycState kycState, bool estComplet) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!estComplet) ...[
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.surfaceClaire,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppColors.accent.withValues(alpha: 0.3)),
              ),
              child: const Row(children: [
                Icon(Icons.info_outline, color: AppColors.accent, size: 18),
                SizedBox(width: 10),
                Expanded(child: Text(
                  'Ce niveau (KYC 1) vous permet de publier des demandes de transport.',
                  style: TextStyle(color: AppColors.texteMuet, fontSize: 12),
                )),
              ]),
            ),
            const SizedBox(height: 24),
            _etape(
              numero: 1,
              titre: 'Identité',
              fait: kycState.identiteDeclaree,
              contenu: kycState.identiteDeclaree
                  ? _resumeIdentite(kycState)
                  : _formulaireIdentite(kycState, avecBouton: false),
            ),
            const SizedBox(height: 16),
            _etape(
              numero: 2,
              titre: 'Pièce d\'identité',
              fait: kycState.pieceDeposee,
              contenu: kycState.pieceDeposee ? _resumePieces(kycState) : _boutonDepot(kycState),
            ),
            if (!kycState.identiteDeclaree) ...[
              const SizedBox(height: 16),
              _boutonEnregistrer(kycState),
            ],
          ] else
            _formulaireIdentite(kycState),

          if (kycState.erreur != null) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.erreur.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
              ),
              child: Text(kycState.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 12)),
            ),
          ],
        ],
      ),
    );
  }

  Widget _profilComplete(KycState kycState) {
    final nomAffiche = kycState.type == 'ENTREPRISE'
        ? (kycState.raisonSociale ?? '—')
        : '${kycState.prenom ?? ''} ${kycState.nom ?? ''}'.trim();
    final telephone = ref.watch(authProvider).telephone;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Column(
              children: [
                Container(
                  width: 84, height: 84,
                  decoration: BoxDecoration(color: AppColors.surfaceClaire, shape: BoxShape.circle,
                      border: Border.all(color: AppColors.accent.withValues(alpha: 0.3), width: 2)),
                  child: Icon(kycState.type == 'ENTREPRISE' ? Icons.apartment : Icons.person, color: AppColors.accent, size: 40),
                ),
                const SizedBox(height: 12),
                Text(nomAffiche.isEmpty ? '—' : nomAffiche, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(color: AppColors.succes.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(20)),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    const Icon(Icons.verified, color: AppColors.succes, size: 14),
                    const SizedBox(width: 4),
                    Text(kycState.niveauKyc, style: const TextStyle(color: AppColors.succes, fontSize: 12, fontWeight: FontWeight.w600)),
                  ]),
                ),
              ],
            ),
          ),
          const SizedBox(height: 28),
          _ligneInfo(Icons.phone_outlined, 'Téléphone', telephone ?? '—', onEdit: () => _modifierTelephone(telephone)),
          _ligneInfo(Icons.badge_outlined, 'Type de compte', kycState.type == 'ENTREPRISE' ? 'Entreprise' : 'Particulier'),
          if (kycState.pieces.isNotEmpty)
            _ligneInfo(Icons.description_outlined, 'Pièce déposée', kycState.pieces.first.typeDocument),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: OutlinedButton.icon(
              onPressed: () => _ouvrirEdition(kycState),
              icon: const Icon(Icons.edit_outlined, color: AppColors.accent, size: 18),
              label: const Text('Modifier', style: TextStyle(color: AppColors.accent, fontWeight: FontWeight.w600)),
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: AppColors.accent),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _ligneInfo(IconData icone, String label, String valeur, {VoidCallback? onEdit}) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(color: AppColors.surface, borderRadius: BorderRadius.circular(10), border: Border.all(color: AppColors.bordure)),
      child: Row(
        children: [
          Icon(icone, color: AppColors.texteMuet, size: 20),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: const TextStyle(fontSize: 11, color: AppColors.texteMuet, letterSpacing: 0.5)),
                Text(valeur, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
              ],
            ),
          ),
          if (onEdit != null)
            IconButton(
              icon: const Icon(Icons.edit_outlined, color: AppColors.accent, size: 18),
              onPressed: onEdit,
              tooltip: 'Modifier',
            ),
        ],
      ),
    );
  }

  // Changement du numéro de téléphone (identifiant de connexion) : l'ancien
  // numéro doit être re-saisi et confirmé côté serveur avant d'accepter le
  // nouveau — évite qu'un tiers ayant accès à l'appareil déverrouillé ne
  // s'approprie silencieusement le compte.
  Future<void> _modifierTelephone(String? telephoneActuel) async {
    final ancienCtrl = TextEditingController();
    final nouveauCtrl = TextEditingController();
    final formKey = GlobalKey<FormState>();

    final confirme = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Modifier le numéro de téléphone'),
        content: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Numéro actuel : ${telephoneActuel ?? '—'}',
                  style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
              const SizedBox(height: 16),
              TextFormField(
                controller: ancienCtrl,
                keyboardType: TextInputType.phone,
                decoration: _decoration('Confirmez votre numéro actuel', Icons.phone_outlined),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Champ obligatoire' : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: nouveauCtrl,
                keyboardType: TextInputType.phone,
                decoration: _decoration('Nouveau numéro', Icons.phone_iphone_outlined),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Champ obligatoire' : null,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Annuler')),
          ElevatedButton(
            onPressed: () {
              if (formKey.currentState!.validate()) Navigator.pop(dialogContext, true);
            },
            child: const Text('Valider'),
          ),
        ],
      ),
    );

    if (confirme != true || !mounted) return;

    final succes = await ref.read(authProvider.notifier).modifierTelephone(ancienCtrl.text.trim(), nouveauCtrl.text.trim());
    if (!mounted) return;
    final erreur = ref.read(authProvider).erreur;
    afficherNotification(
      context,
      message: succes ? 'Numéro de téléphone mis à jour.' : (erreur ?? 'Échec de la modification.'),
      couleur: succes ? AppColors.succes : AppColors.erreur,
      icone: succes ? Icons.check_circle : Icons.error_outline,
    );
  }

  Widget _etape({required int numero, required String titre, required bool fait, required Widget contenu}) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: fait ? AppColors.succes.withValues(alpha: 0.4) : AppColors.bordure),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            Icon(fait ? Icons.check_circle : Icons.radio_button_unchecked,
                color: fait ? AppColors.succes : AppColors.texteMuet, size: 20),
            const SizedBox(width: 8),
            Text('$numero. $titre', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
          ]),
          const SizedBox(height: 12),
          contenu,
        ],
      ),
    );
  }

  Widget _resumeIdentite(KycState kycState) {
    return Text(
      kycState.type == 'ENTREPRISE'
          ? (kycState.raisonSociale ?? '—')
          : '${kycState.prenom ?? ''} ${kycState.nom ?? ''}'.trim(),
      style: const TextStyle(color: AppColors.texteMuet),
    );
  }

  Widget _resumePieces(KycState kycState) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: kycState.pieces
          .map((p) => Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text('• ${p.typeDocument}', style: const TextStyle(color: AppColors.texteMuet)),
              ))
          .toList(),
    );
  }

  Widget _boutonDepot(KycState kycState) {
    final occupe = kycState.depotEnCours || _verificationEnCours;
    return SizedBox(
      width: double.infinity,
      height: 48,
      child: OutlinedButton.icon(
        onPressed: occupe ? null : _deposerPiece,
        icon: occupe
            ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2.5))
            : const Icon(Icons.camera_alt_outlined, color: AppColors.accent),
        label: Text(
            _verificationEnCours ? 'Vérification…' : (kycState.depotEnCours ? 'Envoi…' : 'Prendre en photo ma pièce d\'identité'),
            style: const TextStyle(color: AppColors.accent, fontWeight: FontWeight.w600)),
        style: OutlinedButton.styleFrom(
          side: const BorderSide(color: AppColors.accent),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        ),
      ),
    );
  }

  Widget _boutonEnregistrer(KycState kycState) {
    return SizedBox(
      width: double.infinity,
      height: 48,
      child: ElevatedButton(
        onPressed: kycState.chargement ? null : _enregistrerIdentite,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.accent,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        ),
        child: kycState.chargement
            ? const SizedBox(height: 20, width: 20,
                child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
            : const Text('Enregistrer', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
      ),
    );
  }

  Widget _formulaireIdentite(KycState kycState, {bool avecBouton = true}) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            Expanded(
              child: OutlinedButton(
                onPressed: () => setState(() => _type = _TypeProfil.particulier),
                style: OutlinedButton.styleFrom(
                  backgroundColor: _type == _TypeProfil.particulier ? AppColors.accent : AppColors.surface,
                  side: BorderSide(color: _type == _TypeProfil.particulier ? AppColors.accent : AppColors.bordure),
                ),
                child: Text('Particulier',
                    style: TextStyle(color: _type == _TypeProfil.particulier ? Colors.white : AppColors.texte)),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: OutlinedButton(
                onPressed: () => setState(() => _type = _TypeProfil.entreprise),
                style: OutlinedButton.styleFrom(
                  backgroundColor: _type == _TypeProfil.entreprise ? AppColors.accent : AppColors.surface,
                  side: BorderSide(color: _type == _TypeProfil.entreprise ? AppColors.accent : AppColors.bordure),
                ),
                child: Text('Entreprise',
                    style: TextStyle(color: _type == _TypeProfil.entreprise ? Colors.white : AppColors.texte)),
              ),
            ),
          ]),
          const SizedBox(height: 16),

          if (_type == _TypeProfil.entreprise) ...[
            _label('RAISON SOCIALE'),
            TextFormField(
              controller: _raisonSocialeCtrl,
              decoration: _decoration('Ex : Cimencam SA', Icons.apartment),
              validator: (v) => (v == null || v.isEmpty) ? 'Raison sociale obligatoire' : null,
            ),
            const SizedBox(height: 16),
            _label('NUMÉRO RCCM (optionnel)'),
            TextFormField(
              controller: _rccmCtrl,
              decoration: _decoration('Ex : RC/DLA/2024/B/1234', Icons.badge_outlined),
            ),
          ] else ...[
            _label('PRÉNOM'),
            TextFormField(
              controller: _prenomCtrl,
              decoration: _decoration('Ex : Awa', Icons.person),
              validator: (v) => (v == null || v.isEmpty) ? 'Prénom obligatoire' : null,
            ),
            const SizedBox(height: 16),
            _label('NOM'),
            TextFormField(
              controller: _nomCtrl,
              decoration: _decoration('Ex : Mballa', Icons.person_outline),
              validator: (v) => (v == null || v.isEmpty) ? 'Nom obligatoire' : null,
            ),
          ],
          if (avecBouton) ...[
            const SizedBox(height: 16),
            _boutonEnregistrer(kycState),
          ],
        ],
      ),
    );
  }
}
