import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl_phone_field/intl_phone_field.dart';
import '../l10n/app_localizations.dart';
import '../providers/auth_provider.dart';
import '../providers/tenant_selection_provider.dart';
import '../theme/app_theme.dart';
import '../widgets/top_notification.dart';
import 'inscription_screen.dart';
import 'kyc_screen.dart';
import 'tenant_selection_screen.dart';

// FE-WEB-01/S1 : écran de connexion unique, aucune indication de rôle avant
// authentification réussie — le rôle (CHAUFFEUR/TRANSPORTEUR/AGENT/
// CHAUFFEUR_PROPRIETAIRE) est résolu depuis le JWT retourné par le gateway,
// jamais choisi côté client.
//
// Carte blanche flottante sur l'illustration de fond (même identité que
// welcome_screen.dart) plutôt qu'un simple fond blanc plat.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _codeCtrl = TextEditingController();
  String _telephoneComplet = '';
  bool _codeVisible = false;

  @override
  void dispose() {
    _codeCtrl.dispose();
    super.dispose();
  }

  static const _rolesAutorises = ['CHAUFFEUR', 'TRANSPORTEUR', 'CHAUFFEUR_PROPRIETAIRE', 'AGENT'];

  Future<void> _login() async {
    if (!_formKey.currentState!.validate() || _telephoneComplet.isEmpty) return;
    final succes = await ref.read(authProvider.notifier).login(
      _telephoneComplet,
      _codeCtrl.text.trim(),
    );
    if (!succes || !mounted) return;

    // Le gateway authentifie tous les rôles connus (CHARGEUR compris, pour
    // l'app Client) — cette app reste réservée à Chauffeur/Transporteur/Agent,
    // un compte d'un autre rôle est refusé ici plutôt que d'atterrir sur un
    // accueil vide sans explication.
    final role = ref.read(authProvider).role;
    if (!_rolesAutorises.contains(role)) {
      await ref.read(authProvider.notifier).logout();
      if (!mounted) return;
      afficherNotification(
        context,
        message: AppLocalizations.of(context).compteClientMessage,
        couleur: AppColors.erreur,
        icone: Icons.error_outline,
      );
      return;
    }

    // S18 (audit de suivi, 23 aout) — appel reel : selection de tenant
    // seulement si le compte est effectivement affilie a plusieurs bureaux
    // (GET /api/v1/auth/tenants) ; sinon comportement inchange, direct au KYC.
    final besoinSelectionTenant = await ref.read(tenantSelectionProvider.notifier).resoudrePourCompte();
    if (!mounted) return;

    Navigator.pushReplacement(
      context,
      MaterialPageRoute(builder: (_) => besoinSelectionTenant ? const TenantSelectionScreen() : const KycScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final t = AppLocalizations.of(context);

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
              constraints: BoxConstraints(maxHeight: MediaQuery.of(context).size.height * 0.82),
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
                      Center(child: Image.asset('assets/images/logo_fretcorridor.jpeg', height: 36)),
                      const SizedBox(height: 16),
                      Text(t.seConnecter, textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.headlineMedium),
                      const SizedBox(height: 24),

                      Text(t.champTelephone, style: const TextStyle(fontSize: 11, letterSpacing: 1.2,
                          color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
                      const SizedBox(height: 8),
                      IntlPhoneField(
                        initialCountryCode: 'CM',
                        dropdownTextStyle: const TextStyle(color: AppColors.texte),
                        style: const TextStyle(color: AppColors.texte, fontSize: 15),
                        decoration: _decoration(),
                        onChanged: (phone) => _telephoneComplet = phone.completeNumber,
                      ),
                      const SizedBox(height: 20),

                      Text(t.champCode, style: const TextStyle(fontSize: 11, letterSpacing: 1.2,
                          color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
                      const SizedBox(height: 8),
                      TextFormField(
                        controller: _codeCtrl,
                        obscureText: !_codeVisible,
                        keyboardType: TextInputType.number,
                        maxLength: 6,
                        style: const TextStyle(color: AppColors.texte, fontSize: 20, letterSpacing: 8),
                        decoration: _decoration(hint: '••••').copyWith(
                          counterText: '',
                          prefixIcon: const Icon(Icons.lock, color: AppColors.texteMuet),
                          suffixIcon: IconButton(
                            icon: Icon(_codeVisible ? Icons.visibility_off : Icons.visibility, color: AppColors.texteMuet),
                            onPressed: () => setState(() => _codeVisible = !_codeVisible),
                          ),
                        ),
                        validator: (v) {
                          if (v == null || v.isEmpty) return t.codeObligatoire;
                          if (!RegExp(r'^[0-9]{4,6}$').hasMatch(v)) return t.codeFormatInvalide;
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
                          onPressed: authState.chargement ? null : _login,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppColors.accent,
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          ),
                          child: authState.chargement
                              ? const SizedBox(height: 22, width: 22,
                                  child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                              : Text(t.seConnecter,
                                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
                        ),
                      ),
                      const SizedBox(height: 16),
                      Center(
                        child: TextButton(
                          onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const InscriptionScreen())),
                          child: Text(t.creerUnCompte,
                              style: const TextStyle(color: AppColors.accent, fontWeight: FontWeight.w600)),
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

  InputDecoration _decoration({String? hint}) => InputDecoration(
        hintText: hint,
        filled: true,
        fillColor: AppColors.fond,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.accent)),
      );
}
