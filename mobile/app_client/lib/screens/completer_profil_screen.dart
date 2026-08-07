import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/kyc_provider.dart';
import '../theme/app_theme.dart';

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
  bool _entreprise = false;

  @override
  void dispose() {
    _nomCtrl.dispose(); _prenomCtrl.dispose();
    _raisonSocialeCtrl.dispose(); _rccmCtrl.dispose();
    super.dispose();
  }

  Future<void> _valider() async {
    if (!_formKey.currentState!.validate()) return;
    final succes = _entreprise
        ? await ref.read(kycProvider.notifier).completerEntreprise(
            raisonSociale: _raisonSocialeCtrl.text.trim(),
            numeroRegistreCommerce: _rccmCtrl.text.trim().isEmpty ? null : _rccmCtrl.text.trim(),
          )
        : await ref.read(kycProvider.notifier).completerParticulier(
            nom: _nomCtrl.text.trim(),
            prenom: _prenomCtrl.text.trim(),
          );
    if (succes && mounted) Navigator.pop(context);
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

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Compléter mon profil')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
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

              Row(children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => setState(() => _entreprise = false),
                    style: OutlinedButton.styleFrom(
                      backgroundColor: !_entreprise ? AppColors.accent : AppColors.surface,
                      side: BorderSide(color: !_entreprise ? AppColors.accent : AppColors.bordure),
                    ),
                    child: Text('Particulier', style: TextStyle(color: !_entreprise ? Colors.white : AppColors.texte)),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => setState(() => _entreprise = true),
                    style: OutlinedButton.styleFrom(
                      backgroundColor: _entreprise ? AppColors.accent : AppColors.surface,
                      side: BorderSide(color: _entreprise ? AppColors.accent : AppColors.bordure),
                    ),
                    child: Text('Entreprise', style: TextStyle(color: _entreprise ? Colors.white : AppColors.texte)),
                  ),
                ),
              ]),
              const SizedBox(height: 20),

              if (_entreprise) ...[
                _label('RAISON SOCIALE'),
                TextFormField(
                  controller: _raisonSocialeCtrl,
                  decoration: _decoration('Ex : Cimencam SA', Icons.apartment),
                  validator: (v) => (_entreprise && (v == null || v.isEmpty)) ? 'Raison sociale obligatoire' : null,
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
                  validator: (v) => (!_entreprise && (v == null || v.isEmpty)) ? 'Prénom obligatoire' : null,
                ),
                const SizedBox(height: 16),
                _label('NOM'),
                TextFormField(
                  controller: _nomCtrl,
                  decoration: _decoration('Ex : Mballa', Icons.person_outline),
                  validator: (v) => (!_entreprise && (v == null || v.isEmpty)) ? 'Nom obligatoire' : null,
                ),
              ],
              const SizedBox(height: 24),

              if (kycState.erreur != null) ...[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.erreur.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
                  ),
                  child: Text(kycState.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 12)),
                ),
                const SizedBox(height: 16),
              ],

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
                      ? const SizedBox(height: 22, width: 22,
                          child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                      : const Text('Valider', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
