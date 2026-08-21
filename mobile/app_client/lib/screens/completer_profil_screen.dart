import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import '../providers/auth_provider.dart';
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
    await ref.read(kycProvider.notifier).deposerDocument('CNI', File(image.path));
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
          _ligneInfo(Icons.phone_outlined, 'Téléphone', telephone ?? '—'),
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

  Widget _ligneInfo(IconData icone, String label, String valeur) {
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
        ],
      ),
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
    return SizedBox(
      width: double.infinity,
      height: 48,
      child: OutlinedButton.icon(
        onPressed: kycState.depotEnCours ? null : _deposerPiece,
        icon: kycState.depotEnCours
            ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2.5))
            : const Icon(Icons.camera_alt_outlined, color: AppColors.accent),
        label: Text(kycState.depotEnCours ? 'Envoi…' : 'Prendre en photo ma pièce d\'identité',
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
