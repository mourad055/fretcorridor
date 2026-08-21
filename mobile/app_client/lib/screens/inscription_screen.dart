import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl_phone_field/intl_phone_field.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import 'home_placeholder_screen.dart';

class InscriptionScreen extends ConsumerStatefulWidget {
  const InscriptionScreen({super.key});

  @override
  ConsumerState<InscriptionScreen> createState() => _InscriptionScreenState();
}

class _InscriptionScreenState extends ConsumerState<InscriptionScreen> {
  final _formKey = GlobalKey<FormState>();
  final _pinCtrl = TextEditingController();
  final _nomCtrl = TextEditingController();
  final _prenomCtrl = TextEditingController();
  final _raisonSocialeCtrl = TextEditingController();
  String _telephoneComplet = '';

  bool _entreprise = false;

  @override
  void dispose() {
    _pinCtrl.dispose();
    _nomCtrl.dispose(); _prenomCtrl.dispose(); _raisonSocialeCtrl.dispose();
    super.dispose();
  }

  Future<void> _inscrire() async {
    if (!_formKey.currentState!.validate() || _telephoneComplet.isEmpty) return;
    final succes = await ref.read(authProvider.notifier).inscrireChargeur(
      telephone: _telephoneComplet,
      codePin: _pinCtrl.text.trim(),
      nom: _entreprise ? null : _nomCtrl.text.trim(),
      prenom: _entreprise ? null : _prenomCtrl.text.trim(),
      raisonSociale: _entreprise ? _raisonSocialeCtrl.text.trim() : null,
    );
    if (succes && mounted) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const HomePlaceholderScreen()),
      );
    }
  }

  InputDecoration _decoration(String hint, IconData icon) {
    return InputDecoration(
      hintText: hint,
      filled: true,
      fillColor: AppColors.surface,
      prefixIcon: Icon(icon, color: AppColors.texteMuet, size: 20),
      border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.bordure)),
      enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.bordure)),
      focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.accent)),
    );
  }

  Widget _label(String text) => Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Text(text, style: const TextStyle(fontSize: 11, letterSpacing: 1.1,
            color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
      );

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.texte),
          onPressed: () => Navigator.pop(context),
        ),
        title: Row(
          children: [
            Image.asset('assets/images/logo_fretcorridor.jpeg', height: 28),
            const SizedBox(width: 8),
            const Text('Créer un compte', style: TextStyle(fontSize: 16)),
          ],
        ),
      ),
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
                    'Inscription rapide — vous pourrez compléter votre profil plus tard.',
                    style: TextStyle(color: AppColors.texteMuet, fontSize: 12),
                  )),
                ]),
              ),
              const SizedBox(height: 24),

              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => setState(() => _entreprise = false),
                      style: OutlinedButton.styleFrom(
                        backgroundColor: !_entreprise ? AppColors.accent : AppColors.surface,
                        side: BorderSide(color: !_entreprise ? AppColors.accent : AppColors.bordure),
                      ),
                      child: Text('Particulier',
                          style: TextStyle(color: !_entreprise ? Colors.white : AppColors.texte)),
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
                      child: Text('Entreprise',
                          style: TextStyle(color: _entreprise ? Colors.white : AppColors.texte)),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),

              if (_entreprise) ...[
                _label('RAISON SOCIALE'),
                TextFormField(
                  controller: _raisonSocialeCtrl,
                  decoration: _decoration('Ex : Cimencam SA', Icons.apartment),
                  validator: (v) => (_entreprise && (v == null || v.isEmpty))
                      ? 'Raison sociale obligatoire' : null,
                ),
              ] else ...[
                _label('PRÉNOM'),
                TextFormField(
                  controller: _prenomCtrl,
                  decoration: _decoration('Ex : Awa', Icons.person),
                  validator: (v) => (!_entreprise && (v == null || v.isEmpty))
                      ? 'Prénom obligatoire' : null,
                ),
                const SizedBox(height: 16),
                _label('NOM'),
                TextFormField(
                  controller: _nomCtrl,
                  decoration: _decoration('Ex : Mballa', Icons.person_outline),
                  validator: (v) => (!_entreprise && (v == null || v.isEmpty))
                      ? 'Nom obligatoire' : null,
                ),
              ],
              const SizedBox(height: 16),

              _label('TÉLÉPHONE'),
              IntlPhoneField(
                initialCountryCode: 'CM',
                dropdownTextStyle: const TextStyle(color: AppColors.texte),
                decoration: _decoration('', Icons.phone).copyWith(hintText: null),
                onChanged: (phone) => _telephoneComplet = phone.completeNumber,
              ),
              const SizedBox(height: 16),

              _label('CODE PIN (4 à 6 chiffres)'),
              TextFormField(
                controller: _pinCtrl,
                obscureText: true,
                keyboardType: TextInputType.number,
                maxLength: 6,
                decoration: _decoration('Ex : 1234', Icons.lock).copyWith(counterText: ''),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'PIN obligatoire';
                  if (!RegExp(r'^[0-9]{4,6}$').hasMatch(v)) return '4 à 6 chiffres';
                  return null;
                },
              ),
              const SizedBox(height: 20),

              if (authState.erreur != null) ...[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.erreur.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
                  ),
                  child: Row(children: [
                    const Icon(Icons.error_outline, color: AppColors.erreur, size: 18),
                    const SizedBox(width: 8),
                    Expanded(child: Text(authState.erreur!,
                        style: const TextStyle(color: AppColors.erreur, fontSize: 12))),
                  ]),
                ),
                const SizedBox(height: 16),
              ],

              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton(
                  onPressed: authState.chargement ? null : _inscrire,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.accent,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: authState.chargement
                      ? const SizedBox(height: 22, width: 22,
                          child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                      : const Text('Créer mon compte',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
