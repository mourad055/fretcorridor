import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import 'home_screen.dart';

// FE-WEB-01/S1 : écran de connexion unique, aucune indication de rôle avant
// authentification réussie — le rôle (CHAUFFEUR/TRANSPORTEUR/AGENT/
// CHAUFFEUR_PROPRIETAIRE) est résolu depuis le JWT retourné par le gateway,
// jamais choisi côté client.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _telCtrl = TextEditingController();
  final _codeCtrl = TextEditingController();
  bool _codeVisible = false;

  @override
  void dispose() {
    _telCtrl.dispose();
    _codeCtrl.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate()) return;
    final succes = await ref.read(authProvider.notifier).login(
      _telCtrl.text.trim(),
      _codeCtrl.text.trim(),
    );
    if (succes && mounted) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const HomeScreen()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);

    return Scaffold(
      backgroundColor: AppColors.fond,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 64),
                Text('FretCorridor', style: Theme.of(context).textTheme.headlineLarge),
                const SizedBox(height: 4),
                const Text('Chauffeur / Transporteur',
                    style: TextStyle(fontSize: 14, color: AppColors.texteMuet)),
                const SizedBox(height: 48),

                const Text('TÉLÉPHONE', style: TextStyle(fontSize: 11, letterSpacing: 1.2,
                    color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _telCtrl,
                  keyboardType: TextInputType.phone,
                  style: const TextStyle(color: AppColors.texte, fontSize: 15),
                  decoration: InputDecoration(
                    hintText: '+237 6XX XXX XXX',
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
                    prefixIcon: const Icon(Icons.phone, color: AppColors.texteMuet),
                  ),
                  validator: (v) {
                    if (v == null || v.isEmpty) return 'Téléphone obligatoire';
                    if (!RegExp(r'^\+?[0-9]{9,15}$').hasMatch(v)) return 'Format invalide';
                    return null;
                  },
                ),
                const SizedBox(height: 20),

                const Text('CODE', style: TextStyle(fontSize: 11, letterSpacing: 1.2,
                    color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _codeCtrl,
                  obscureText: !_codeVisible,
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
                      icon: Icon(_codeVisible ? Icons.visibility_off : Icons.visibility,
                          color: AppColors.texteMuet),
                      onPressed: () => setState(() => _codeVisible = !_codeVisible),
                    ),
                  ),
                  validator: (v) {
                    if (v == null || v.isEmpty) return 'Code obligatoire';
                    if (!RegExp(r'^[0-9]{4,6}$').hasMatch(v)) return '4 à 6 chiffres';
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
                        : const Text('Se connecter',
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold,
                                color: AppColors.texteBouton)),
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
