import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl_phone_field/intl_phone_field.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import 'kyc_screen.dart';

enum _TypeCompte { chauffeur, transporteur, chauffeurProprietaire }

extension on _TypeCompte {
  String get valeurApi => switch (this) {
        _TypeCompte.chauffeur => 'CHAUFFEUR',
        _TypeCompte.transporteur => 'TRANSPORTEUR',
        _TypeCompte.chauffeurProprietaire => 'CHAUFFEUR_PROPRIETAIRE',
      };

  String get libelle => switch (this) {
        _TypeCompte.chauffeur => 'Chauffeur',
        _TypeCompte.transporteur => 'Transporteur',
        _TypeCompte.chauffeurProprietaire => 'Les deux',
      };
}

// Même carte flottante sur l'illustration de fond que login_screen.dart —
// identité visuelle continue entre accueil / connexion / inscription.
class InscriptionScreen extends ConsumerStatefulWidget {
  const InscriptionScreen({super.key});

  @override
  ConsumerState<InscriptionScreen> createState() => _InscriptionScreenState();
}

class _InscriptionScreenState extends ConsumerState<InscriptionScreen> {
  final _formKey = GlobalKey<FormState>();
  final _codeCtrl = TextEditingController();
  final _nomCtrl = TextEditingController();
  final _prenomCtrl = TextEditingController();
  final _raisonSocialeCtrl = TextEditingController();
  String _telephoneComplet = '';
  _TypeCompte _type = _TypeCompte.chauffeur;
  bool _codeVisible = false;

  @override
  void dispose() {
    _codeCtrl.dispose();
    _nomCtrl.dispose();
    _prenomCtrl.dispose();
    _raisonSocialeCtrl.dispose();
    super.dispose();
  }

  Future<void> _creerCompte() async {
    if (!_formKey.currentState!.validate() || _telephoneComplet.isEmpty) return;
    final succes = await ref.read(authProvider.notifier).register(
          telephone: _telephoneComplet,
          code: _codeCtrl.text.trim(),
          type: _type.valeurApi,
          nom: _type == _TypeCompte.transporteur ? null : _nomCtrl.text.trim(),
          prenom: _type == _TypeCompte.transporteur ? null : _prenomCtrl.text.trim(),
          raisonSociale: _type == _TypeCompte.transporteur ? _raisonSocialeCtrl.text.trim() : null,
        );
    if (succes && mounted) {
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const KycScreen()));
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);

    return Scaffold(
      backgroundColor: AppColors.texte,
      body: Stack(
        fit: StackFit.expand,
        children: [
          Image.asset('assets/images/hero_illustration.png', fit: BoxFit.cover),
          Container(color: Colors.black.withValues(alpha: 0.45)),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.only(left: 12, top: 4),
              child: IconButton(
                icon: const Icon(Icons.arrow_back, color: Colors.white),
                onPressed: () => Navigator.pop(context),
              ),
            ),
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: Container(
              width: double.infinity,
              constraints: BoxConstraints(maxHeight: MediaQuery.of(context).size.height * 0.88),
              padding: const EdgeInsets.fromLTRB(24, 28, 24, 24),
              decoration: const BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
              ),
              child: SingleChildScrollView(
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Center(child: Image.asset('assets/images/logo_fretcorridor.jpeg', height: 32)),
                      const SizedBox(height: 12),
                      Text('Créer un compte', textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.headlineMedium),
                      const SizedBox(height: 4),
                      const Text('Vous compléterez votre profil juste après.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                      const SizedBox(height: 20),

                      const Text('JE SUIS', style: TextStyle(fontSize: 11, letterSpacing: 1.2,
                          color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
                      const SizedBox(height: 8),
                      Wrap(spacing: 8, runSpacing: 8, children: _TypeCompte.values.map((t) {
                        final selectionne = t == _type;
                        return ChoiceChip(
                          label: Text(t.libelle),
                          selected: selectionne,
                          onSelected: (_) => setState(() => _type = t),
                          selectedColor: AppColors.accent,
                          backgroundColor: AppColors.fond,
                          labelStyle: TextStyle(color: selectionne ? Colors.white : AppColors.texte),
                          side: BorderSide(color: selectionne ? AppColors.accent : AppColors.bordure),
                        );
                      }).toList()),
                      const SizedBox(height: 20),

                      if (_type == _TypeCompte.transporteur) ...[
                        _label('RAISON SOCIALE'),
                        const SizedBox(height: 8),
                        TextFormField(
                          controller: _raisonSocialeCtrl,
                          decoration: _decoration('Ex : Transport Fotso SARL'),
                          validator: (v) => (v == null || v.isEmpty) ? 'Raison sociale obligatoire' : null,
                        ),
                        const SizedBox(height: 16),
                      ] else ...[
                        _label('PRÉNOM'),
                        const SizedBox(height: 8),
                        TextFormField(
                          controller: _prenomCtrl,
                          decoration: _decoration('Ex : Paul'),
                          validator: (v) => (v == null || v.isEmpty) ? 'Prénom obligatoire' : null,
                        ),
                        const SizedBox(height: 16),
                        _label('NOM'),
                        const SizedBox(height: 8),
                        TextFormField(
                          controller: _nomCtrl,
                          decoration: _decoration('Ex : Kamga'),
                          validator: (v) => (v == null || v.isEmpty) ? 'Nom obligatoire' : null,
                        ),
                        const SizedBox(height: 16),
                      ],

                      _label('TÉLÉPHONE'),
                      const SizedBox(height: 8),
                      IntlPhoneField(
                        initialCountryCode: 'CM',
                        decoration: _decoration(null),
                        dropdownTextStyle: const TextStyle(color: AppColors.texte),
                        style: const TextStyle(color: AppColors.texte, fontSize: 15),
                        onChanged: (phone) => _telephoneComplet = phone.completeNumber,
                      ),
                      const SizedBox(height: 16),

                      _label('CODE (4 à 6 chiffres)'),
                      const SizedBox(height: 8),
                      TextFormField(
                        controller: _codeCtrl,
                        obscureText: !_codeVisible,
                        keyboardType: TextInputType.number,
                        maxLength: 6,
                        style: const TextStyle(color: AppColors.texte, fontSize: 20, letterSpacing: 8),
                        decoration: _decoration('••••').copyWith(
                          counterText: '',
                          prefixIcon: const Icon(Icons.lock, color: AppColors.texteMuet),
                          suffixIcon: IconButton(
                            icon: Icon(_codeVisible ? Icons.visibility_off : Icons.visibility, color: AppColors.texteMuet),
                            onPressed: () => setState(() => _codeVisible = !_codeVisible),
                          ),
                        ),
                        validator: (v) {
                          if (v == null || v.isEmpty) return 'Code obligatoire';
                          if (!RegExp(r'^[0-9]{4,6}$').hasMatch(v)) return '4 à 6 chiffres';
                          return null;
                        },
                      ),

                      if (authState.erreur != null) ...[
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
                            Expanded(child: Text(authState.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 13))),
                          ]),
                        ),
                      ],
                      const SizedBox(height: 20),

                      SizedBox(
                        height: 52,
                        child: ElevatedButton(
                          onPressed: authState.chargement ? null : _creerCompte,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppColors.accent,
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          ),
                          child: authState.chargement
                              ? const SizedBox(height: 22, width: 22,
                                  child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                              : const Text('Créer mon compte',
                                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _label(String text) => Text(text,
      style: const TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600));

  InputDecoration _decoration(String? hint) => InputDecoration(
        hintText: hint,
        filled: true,
        fillColor: AppColors.fond,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.accent)),
      );
}
