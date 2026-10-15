import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl_phone_field/intl_phone_field.dart';
import '../l10n/app_localizations.dart';
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
          message: AppLocalizations.of(context).pieceInvalideMessage,
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

  // sansIcone : les champs IntlPhoneField affichent deja leur propre
  // indicatif pays en tete de champ -- un prefixIcon supplementaire
  // entrerait en collision visuelle avec celui-ci.
  InputDecoration _decoration(String hint, IconData icon, {bool sansIcone = false}) {
    return InputDecoration(
      hintText: hint,
      filled: true,
      fillColor: AppColors.surface,
      prefixIcon: sansIcone ? null : Icon(icon, color: AppColors.texteMuet, size: 20),
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
    final t = AppLocalizations.of(context);

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
                    Text(t.monProfil, style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: Colors.white)),
                  ]),
                ],
              ),
            ),
          ),
          Expanded(
            child: estComplet && !_modeEdition ? _profilComplete(t, kycState) : _formulaireOuChecklist(t, kycState, estComplet),
          ),
        ],
      ),
    );
  }

  Widget _formulaireOuChecklist(AppLocalizations t, KycState kycState, bool estComplet) {
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
              child: Row(children: [
                const Icon(Icons.info_outline, color: AppColors.accent, size: 18),
                const SizedBox(width: 10),
                Expanded(child: Text(
                  t.kyc1Info,
                  style: const TextStyle(color: AppColors.texteMuet, fontSize: 12),
                )),
              ]),
            ),
            const SizedBox(height: 24),
            _etape(
              numero: 1,
              titre: t.etapeIdentite,
              fait: kycState.identiteDeclaree,
              contenu: kycState.identiteDeclaree
                  ? _resumeIdentite(kycState)
                  : _formulaireIdentite(t, kycState, avecBouton: false),
            ),
            const SizedBox(height: 16),
            _etape(
              numero: 2,
              titre: t.etapePieceIdentite,
              fait: kycState.pieceDeposee,
              contenu: kycState.pieceDeposee ? _resumePieces(kycState) : _boutonDepot(t, kycState),
            ),
            if (!kycState.identiteDeclaree) ...[
              const SizedBox(height: 16),
              _boutonEnregistrer(t, kycState),
            ],
          ] else
            _formulaireIdentite(t, kycState),

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

  Widget _profilComplete(AppLocalizations t, KycState kycState) {
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
          _ligneInfo(t, Icons.phone_outlined, t.phoneLabel, telephone ?? '—', onEdit: () => _modifierTelephone(t, telephone)),
          _ligneInfo(t, Icons.badge_outlined, t.typeDeCompte, kycState.type == 'ENTREPRISE' ? t.entreprise : t.particulier),
          if (kycState.pieces.isNotEmpty)
            _ligneInfo(t, Icons.description_outlined, t.pieceDeposeeLabel, kycState.pieces.first.typeDocument),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: OutlinedButton.icon(
              onPressed: () => _ouvrirEdition(kycState),
              icon: const Icon(Icons.edit_outlined, color: AppColors.accent, size: 18),
              label: Text(t.modifier, style: const TextStyle(color: AppColors.accent, fontWeight: FontWeight.w600)),
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

  Widget _ligneInfo(AppLocalizations t, IconData icone, String label, String valeur, {VoidCallback? onEdit}) {
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
              tooltip: t.modifier,
            ),
        ],
      ),
    );
  }

  // Changement du numéro de téléphone (identifiant de connexion) : l'ancien
  // numéro doit être re-saisi et confirmé côté serveur avant d'accepter le
  // nouveau — évite qu'un tiers ayant accès à l'appareil déverrouillé ne
  // s'approprie silencieusement le compte.
  Future<void> _modifierTelephone(AppLocalizations t, String? telephoneActuel) async {
    // BUG CORRIGE (retour utilisatrice 24/08) : memes TextFormField bruts
    // que cote app Chauffeur, meme correctif -- IntlPhoneField pour la
    // validation par pays native (cf. kyc_screen.dart, app_chauffeur_transporteur).
    String ancienComplet = '';
    String nouveauComplet = '';
    final formKey = GlobalKey<FormState>();

    final confirme = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(t.modifierNumeroTelephone),
        content: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(t.numeroActuel(telephoneActuel ?? '—'),
                  style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
              const SizedBox(height: 16),
              IntlPhoneField(
                initialCountryCode: 'CM',
                decoration: _decoration(t.confirmezNumeroActuel, Icons.phone_outlined, sansIcone: true),
                onChanged: (phone) => ancienComplet = phone.completeNumber,
              ),
              const SizedBox(height: 12),
              IntlPhoneField(
                initialCountryCode: 'CM',
                decoration: _decoration(t.nouveauNumero, Icons.phone_iphone_outlined, sansIcone: true),
                onChanged: (phone) => nouveauComplet = phone.completeNumber,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: Text(t.annuler)),
          ElevatedButton(
            onPressed: () {
              if (ancienComplet.isEmpty || nouveauComplet.isEmpty) return;
              if (formKey.currentState!.validate()) Navigator.pop(dialogContext, true);
            },
            child: Text(t.valider),
          ),
        ],
      ),
    );

    if (confirme != true || !mounted) return;

    final succes = await ref.read(authProvider.notifier).modifierTelephone(ancienComplet, nouveauComplet);
    if (!mounted) return;
    final erreur = ref.read(authProvider).erreur;
    afficherNotification(
      context,
      message: succes ? t.numeroTelephoneMisAJour : (erreur ?? t.echecModification),
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

  Widget _boutonDepot(AppLocalizations t, KycState kycState) {
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
            _verificationEnCours ? t.verificationEnCours : (kycState.depotEnCours ? t.envoiEnCours : t.prendreEnPhotoPiece),
            style: const TextStyle(color: AppColors.accent, fontWeight: FontWeight.w600)),
        style: OutlinedButton.styleFrom(
          side: const BorderSide(color: AppColors.accent),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        ),
      ),
    );
  }

  Widget _boutonEnregistrer(AppLocalizations t, KycState kycState) {
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
            : Text(t.enregistrer, style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
      ),
    );
  }

  Widget _formulaireIdentite(AppLocalizations t, KycState kycState, {bool avecBouton = true}) {
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
                child: Text(t.particulier,
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
                child: Text(t.entreprise,
                    style: TextStyle(color: _type == _TypeProfil.entreprise ? Colors.white : AppColors.texte)),
              ),
            ),
          ]),
          const SizedBox(height: 16),

          if (_type == _TypeProfil.entreprise) ...[
            _label(t.labelRaisonSociale),
            TextFormField(
              controller: _raisonSocialeCtrl,
              decoration: _decoration(t.hintRaisonSociale, Icons.apartment),
              validator: (v) => (v == null || v.isEmpty) ? t.raisonSocialeObligatoire : null,
            ),
            const SizedBox(height: 16),
            _label(t.numeroRccmOptionnel),
            TextFormField(
              controller: _rccmCtrl,
              decoration: _decoration(t.hintRccm, Icons.badge_outlined),
            ),
          ] else ...[
            _label(t.labelPrenom),
            TextFormField(
              controller: _prenomCtrl,
              decoration: _decoration(t.hintPrenom, Icons.person),
              validator: (v) => (v == null || v.isEmpty) ? t.prenomObligatoire : null,
            ),
            const SizedBox(height: 16),
            _label(t.labelNom),
            TextFormField(
              controller: _nomCtrl,
              decoration: _decoration(t.hintNom, Icons.person_outline),
              validator: (v) => (v == null || v.isEmpty) ? t.nomObligatoire : null,
            ),
          ],
          if (avecBouton) ...[
            const SizedBox(height: 16),
            _boutonEnregistrer(t, kycState),
          ],
        ],
      ),
    );
  }
}
