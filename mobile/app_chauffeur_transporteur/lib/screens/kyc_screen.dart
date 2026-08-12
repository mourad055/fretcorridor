import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/kyc_provider.dart';
import '../theme/app_theme.dart';
import 'home_screen.dart';

enum _TypeProfil { particulier, entreprise }

// S2 : KYC gradué niveau 1 (RG-011) — identité déclarée, pas encore de
// pièces justificatives (niveau 2, hors périmètre actuel côté service-ida).
// Le mode Agent (enrôlement terrain) n'a pas non plus de contrat backend
// pour l'instant — écarté de cet écran tant qu'il n'existe pas.
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
  _TypeProfil _type = _TypeProfil.particulier;

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

  Future<void> _valider() async {
    if (!_formKey.currentState!.validate()) return;
    final notifier = ref.read(kycProvider.notifier);
    final succes = _type == _TypeProfil.particulier
        ? await notifier.completerParticulier(_nomCtrl.text.trim(), _prenomCtrl.text.trim())
        : await notifier.completerEntreprise(
            _raisonSocialeCtrl.text.trim(),
            _rccmCtrl.text.trim().isEmpty ? null : _rccmCtrl.text.trim(),
          );
    if (succes && mounted) _continuer();
  }

  void _continuer() {
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const HomeScreen()));
  }

  @override
  Widget build(BuildContext context) {
    final kycState = ref.watch(kycProvider);
    final profil = kycState.profil;

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Mon profil')),
      body: SafeArea(
        child: kycState.chargement && profil == null
            ? const Center(child: CircularProgressIndicator())
            : profil != null && profil.niveauKyc != 'NIVEAU_0'
                ? _profilComplete(profil)
                : _formulaire(kycState),
      ),
    );
  }

  Widget _profilComplete(Profil profil) {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(children: [
            Icon(Icons.check_circle, color: AppColors.succes),
            SizedBox(width: 8),
            Text('Profil complété', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          ]),
          const SizedBox(height: 12),
          Text(
            profil.type == 'ENTREPRISE'
                ? (profil.raisonSociale ?? '—')
                : '${profil.prenom ?? ''} ${profil.nom ?? ''}'.trim(),
            style: const TextStyle(color: AppColors.texteMuet),
          ),
          Text('Niveau KYC : ${profil.niveauKyc}', style: const TextStyle(color: AppColors.texteMuet)),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            height: 52,
            child: ElevatedButton(
              onPressed: _continuer,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: const Text('Continuer',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _formulaire(KycState kycState) {
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 24),
            Text('Complétez votre profil', style: Theme.of(context).textTheme.headlineMedium),
            const SizedBox(height: 4),
            const Text('Identité déclarée — condition pour publier ou accepter une mission.',
                style: TextStyle(fontSize: 13, color: AppColors.texteMuet)),
            const SizedBox(height: 24),

            SegmentedButton<_TypeProfil>(
              segments: const [
                ButtonSegment(value: _TypeProfil.particulier, label: Text('Particulier')),
                ButtonSegment(value: _TypeProfil.entreprise, label: Text('Entreprise')),
              ],
              selected: {_type},
              onSelectionChanged: (s) => setState(() => _type = s.first),
            ),
            const SizedBox(height: 20),

            if (_type == _TypeProfil.particulier) ..._champsParticulier() else ..._champsEntreprise(),

            if (kycState.erreur != null) ...[
              const SizedBox(height: 12),
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
            const SizedBox(height: 20),

            SizedBox(
              width: double.infinity,
              height: 52,
              child: ElevatedButton(
                onPressed: kycState.chargement ? null : _valider,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.accent,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: kycState.chargement
                    ? const SizedBox(
                        height: 22, width: 22, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                    : const Text('Valider',
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
              ),
            ),
            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }

  List<Widget> _champsParticulier() {
    return [
      _champ('NOM', _nomCtrl, obligatoire: true),
      const SizedBox(height: 16),
      _champ('PRÉNOM', _prenomCtrl, obligatoire: true),
    ];
  }

  List<Widget> _champsEntreprise() {
    return [
      _champ('RAISON SOCIALE', _raisonSocialeCtrl, obligatoire: true),
      const SizedBox(height: 16),
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
            fillColor: AppColors.surface,
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
