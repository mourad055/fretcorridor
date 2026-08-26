import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl_phone_field/intl_phone_field.dart';
import '../l10n/app_localizations.dart';
import '../providers/auth_provider.dart';
import '../providers/kyc_provider.dart';
import '../theme/app_theme.dart';
import 'inscription_screen.dart';
import 'home_placeholder_screen.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _pinCtrl = TextEditingController();
  String _telephoneComplet = '';
  bool _pinVisible = false;

  @override
  void dispose() {
    _pinCtrl.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate() || _telephoneComplet.isEmpty) return;
    final succes = await ref.read(authProvider.notifier).login(
      _telephoneComplet,
      _pinCtrl.text.trim(),
    );
    if (succes && mounted) {
      ref.read(kycProvider.notifier).reinitialiser();
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const HomePlaceholderScreen()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final t = AppLocalizations.of(context);

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
            const Text('FretCorridor', style: TextStyle(fontSize: 16)),
          ],
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 24),
                Text(t.seConnecter, style: Theme.of(context).textTheme.headlineLarge),
                const SizedBox(height: 4),
                Text(t.loginSousTitre,
                    style: const TextStyle(fontSize: 14, color: AppColors.texteMuet)),
                const SizedBox(height: 48),

                Text(t.champTelephone, style: const TextStyle(fontSize: 11, letterSpacing: 1.2,
                    color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                IntlPhoneField(
                  initialCountryCode: 'CM',
                  dropdownTextStyle: const TextStyle(color: AppColors.texte),
                  style: const TextStyle(color: AppColors.texte, fontSize: 15),
                  decoration: InputDecoration(
                    filled: true,
                    fillColor: AppColors.surface,
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: const BorderSide(color: AppColors.bordure)),
                    enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: const BorderSide(color: AppColors.bordure)),
                    focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: const BorderSide(color: AppColors.accent)),
                  ),
                  onChanged: (phone) => _telephoneComplet = phone.completeNumber,
                ),
                const SizedBox(height: 20),

                Text(t.champCodePin, style: const TextStyle(fontSize: 11, letterSpacing: 1.2,
                    color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _pinCtrl,
                  obscureText: !_pinVisible,
                  keyboardType: TextInputType.number,
                  maxLength: 6,
                  style: const TextStyle(color: AppColors.texte, fontSize: 20, letterSpacing: 8),
                  decoration: InputDecoration(
                    hintText: '••••',
                    counterText: '',
                    filled: true,
                    fillColor: AppColors.surface,
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: const BorderSide(color: AppColors.bordure)),
                    enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: const BorderSide(color: AppColors.bordure)),
                    focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10),
                        borderSide: const BorderSide(color: AppColors.accent)),
                    prefixIcon: const Icon(Icons.lock, color: AppColors.texteMuet),
                    suffixIcon: IconButton(
                      icon: Icon(_pinVisible ? Icons.visibility_off : Icons.visibility,
                          color: AppColors.texteMuet),
                      onPressed: () => setState(() => _pinVisible = !_pinVisible),
                    ),
                  ),
                  validator: (v) {
                    if (v == null || v.isEmpty) return t.pinObligatoire;
                    if (!RegExp(r'^[0-9]{4,6}$').hasMatch(v)) return t.pinFormatInvalide;
                    return null;
                  },
                ),
                const SizedBox(height: 12),

                if (authState.erreur != null)
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
                      Expanded(child: Text(authState.erreur!,
                          style: const TextStyle(color: AppColors.erreur, fontSize: 13))),
                    ]),
                  ),
                const SizedBox(height: 20),

                SizedBox(
                  width: double.infinity,
                  height: 52,
                  child: ElevatedButton(
                    onPressed: authState.chargement ? null : _login,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.accent,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: authState.chargement
                        ? const SizedBox(height: 22, width: 22,
                            child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                        : Text(t.seConnecter,
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold,
                                color: AppColors.texteBouton)),
                  ),
                ),
                const SizedBox(height: 16),

                Center(
                  child: TextButton(
                    onPressed: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const InscriptionScreen()),
                    ),
                    child: Text(t.pasEncoreDeCompte,
                        style: const TextStyle(color: AppColors.accent, fontSize: 13)),
                  ),
                ),
                const SizedBox(height: 32),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
