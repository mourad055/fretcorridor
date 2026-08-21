import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import '../providers/auth_provider.dart';
import '../providers/kyc_provider.dart';
import '../theme/app_theme.dart';
import 'home_screen.dart';

enum _TypeProfil { particulier, entreprise }

// S2 : KYC gradué niveau 1 (RG-011) — identité déclarée ET au moins une
// pièce déposée, les deux dans n'importe quel ordre. Le niveau 2
// (vérification des pièces) et le mode Agent (enrôlement terrain) n'ont pas
// de contrat backend pour l'instant — écartés de cet écran tant qu'ils
// n'existent pas.
class KycScreen extends ConsumerStatefulWidget {
  const KycScreen({super.key});

  @override
  ConsumerState<KycScreen> createState() => _KycScreenState();
}

class _KycScreenState extends ConsumerState<KycScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nomCtrl = TextEditingController();
  final _prenomCtrl = TextEditingController();
  final _raisonSocialeCtrl = TextEditingController();
  final _rccmCtrl = TextEditingController();
  final _picker = ImagePicker();
  _TypeProfil _type = _TypeProfil.particulier;
  bool _modeEdition = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(kycProvider.notifier).chargerProfil());
  }

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
        ? await notifier.completerParticulier(_nomCtrl.text.trim(), _prenomCtrl.text.trim())
        : await notifier.completerEntreprise(
            _raisonSocialeCtrl.text.trim(),
            _rccmCtrl.text.trim().isEmpty ? null : _rccmCtrl.text.trim(),
          );
    if (succes && mounted) setState(() => _modeEdition = false);
  }

  void _ouvrirEdition(Profil profil) {
    _type = profil.type == 'ENTREPRISE' ? _TypeProfil.entreprise : _TypeProfil.particulier;
    _nomCtrl.text = profil.nom ?? '';
    _prenomCtrl.text = profil.prenom ?? '';
    _raisonSocialeCtrl.text = profil.raisonSociale ?? '';
    setState(() => _modeEdition = true);
  }

  Future<void> _deposerPiece() async {
    final image = await _picker.pickImage(source: ImageSource.camera, imageQuality: 80, maxWidth: 1600);
    if (image == null) return;
    await ref.read(kycProvider.notifier).deposerDocument('CNI', File(image.path));
  }

  @override
  Widget build(BuildContext context) {
    final kycState = ref.watch(kycProvider);
    final profil = kycState.profil;

    // Bascule automatique vers l'accueil dès que le profil devient complet
    // (transition NIVEAU_0 -> NIVEAU_1), avec une notification brève plutôt
    // qu'un écran intermédiaire avec un bouton "Continuer" à trouver.
    ref.listen(kycProvider, (previous, next) {
      final etaitComplet = previous?.profil != null && previous!.profil!.niveauKyc != 'NIVEAU_0';
      final estComplet = next.profil != null && next.profil!.niveauKyc != 'NIVEAU_0';
      if (!etaitComplet && estComplet) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Row(children: [
              Icon(Icons.check_circle, color: Colors.white, size: 20),
              SizedBox(width: 10),
              Text('Profil complété ✅'),
            ]),
            backgroundColor: AppColors.succes,
            behavior: SnackBarBehavior.floating,
          ),
        );
        Future.delayed(const Duration(milliseconds: 600), () {
          if (context.mounted) Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const HomeScreen()));
        });
      }
    });

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
                    Text('Mon profil',
                        style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: Colors.white)),
                  ]),
                ],
              ),
            ),
          ),
          Expanded(
            child: kycState.chargement && profil == null
                ? const Center(child: CircularProgressIndicator())
                : profil != null && profil.niveauKyc != 'NIVEAU_0'
                    ? _profilComplete(kycState, profil)
                    : _checklist(kycState, profil),
          ),
        ],
      ),
    );
  }

  Widget _profilComplete(KycState kycState, Profil profil) {
    if (_modeEdition) {
      return SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: _formulaireIdentite(kycState),
      );
    }

    final nomAffiche = profil.type == 'ENTREPRISE' ? (profil.raisonSociale ?? '—') : '${profil.prenom ?? ''} ${profil.nom ?? ''}'.trim();
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
                  child: Icon(profil.type == 'ENTREPRISE' ? Icons.apartment : Icons.person, color: AppColors.accent, size: 40),
                ),
                const SizedBox(height: 12),
                Text(nomAffiche.isEmpty ? '—' : nomAffiche,
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.succes.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    const Icon(Icons.verified, color: AppColors.succes, size: 14),
                    const SizedBox(width: 4),
                    Text(profil.niveauKyc, style: const TextStyle(color: AppColors.succes, fontSize: 12, fontWeight: FontWeight.w600)),
                  ]),
                ),
              ],
            ),
          ),
          const SizedBox(height: 28),

          _ligneInfo(Icons.phone_outlined, 'Téléphone', telephone ?? '—'),
          _ligneInfo(Icons.badge_outlined, 'Type de compte',
              profil.type == 'ENTREPRISE' ? 'Entreprise' : 'Particulier'),
          if (profil.pieces.isNotEmpty)
            _ligneInfo(Icons.description_outlined, 'Pièce déposée', profil.pieces.first.typeDocument),

          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: OutlinedButton.icon(
              onPressed: () => _ouvrirEdition(profil),
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
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.bordure),
      ),
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

  Widget _checklist(KycState kycState, Profil? profil) {
    final identiteDeclaree = profil?.identiteDeclaree ?? false;
    final pieceDeposee = profil?.pieceDeposee ?? false;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Complétez votre profil', style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 4),
          const Text('Identité déclarée et pièce déposée — condition pour publier ou accepter une mission (RG-011).',
              style: TextStyle(fontSize: 13, color: AppColors.texteMuet)),
          const SizedBox(height: 24),

          _etape(numero: 1, titre: 'Identité', fait: identiteDeclaree,
              contenu: identiteDeclaree
                  ? _resumeIdentite(profil!)
                  : _formulaireIdentite(kycState)),
          const SizedBox(height: 16),
          _etape(numero: 2, titre: 'Pièce d\'identité', fait: pieceDeposee,
              contenu: pieceDeposee ? _resumePieces(profil!) : _boutonDepot(kycState)),

          if (kycState.erreur != null) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.erreur.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
              ),
              child: Row(children: [
                const Icon(Icons.warning_amber, color: AppColors.erreur, size: 18),
                const SizedBox(width: 8),
                Expanded(child: Text(kycState.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 13))),
              ]),
            ),
          ],
          const SizedBox(height: 32),
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

  Widget _resumeIdentite(Profil profil) {
    return Text(
      profil.type == 'ENTREPRISE'
          ? (profil.raisonSociale ?? '—')
          : '${profil.prenom ?? ''} ${profil.nom ?? ''}'.trim(),
      style: const TextStyle(color: AppColors.texteMuet),
    );
  }

  Widget _resumePieces(Profil profil) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: profil.pieces
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

  Widget _formulaireIdentite(KycState kycState) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SegmentedButton<_TypeProfil>(
            segments: const [
              ButtonSegment(value: _TypeProfil.particulier, label: Text('Particulier')),
              ButtonSegment(value: _TypeProfil.entreprise, label: Text('Entreprise')),
            ],
            selected: {_type},
            onSelectionChanged: (s) => setState(() => _type = s.first),
          ),
          const SizedBox(height: 16),

          if (_type == _TypeProfil.particulier) ..._champsParticulier() else ..._champsEntreprise(),
          const SizedBox(height: 16),

          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton(
              onPressed: kycState.chargement ? null : _enregistrerIdentite,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              child: kycState.chargement
                  ? const SizedBox(
                      height: 20, width: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                  : const Text('Enregistrer',
                      style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
            ),
          ),
        ],
      ),
    );
  }

  List<Widget> _champsParticulier() {
    return [
      _champ('NOM', _nomCtrl, obligatoire: true),
      const SizedBox(height: 12),
      _champ('PRÉNOM', _prenomCtrl, obligatoire: true),
    ];
  }

  List<Widget> _champsEntreprise() {
    return [
      _champ('RAISON SOCIALE', _raisonSocialeCtrl, obligatoire: true),
      const SizedBox(height: 12),
      _champ('N° REGISTRE DE COMMERCE (facultatif)', _rccmCtrl, obligatoire: false),
    ];
  }

  Widget _champ(String label, TextEditingController controller, {required bool obligatoire}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: const TextStyle(
                fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
        const SizedBox(height: 8),
        TextFormField(
          controller: controller,
          style: const TextStyle(color: AppColors.texte, fontSize: 15),
          decoration: InputDecoration(
            filled: true,
            fillColor: AppColors.fond,
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
            enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
            focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.accent)),
          ),
          validator: obligatoire ? (v) => (v == null || v.trim().isEmpty) ? 'Champ obligatoire' : null : null,
        ),
      ],
    );
  }
}
