import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/agent_enrolement_provider.dart';
import '../theme/app_theme.dart';

Map<String, String> _typesActeurEnrolables(AppLocalizations t) => {
      'CHAUFFEUR': t.typeChauffeur,
      'TRANSPORTEUR': t.typeTransporteur,
      'CHAUFFEUR_PROPRIETAIRE': t.typeChauffeurProprietaireEnrolement,
    };

// UC-IDA-03 : enrôlement assisté par agent de terrain (EF-IDA-06). RG-019 —
// le PIN est saisi par la personne enrôlée elle-même après réception du
// code par SMS, jamais choisi par l'agent (écart corrigé par rapport à
// l'ancien repo v3, cf. commit backend).
class AgentEnrolementScreen extends ConsumerStatefulWidget {
  const AgentEnrolementScreen({super.key});

  @override
  ConsumerState<AgentEnrolementScreen> createState() => _AgentEnrolementScreenState();
}

class _AgentEnrolementScreenState extends ConsumerState<AgentEnrolementScreen> {
  final _formInitierKey = GlobalKey<FormState>();
  final _telCtrl = TextEditingController();
  String _typeActeur = 'CHAUFFEUR';

  final _formActiverKey = GlobalKey<FormState>();
  final _otpCtrl = TextEditingController();
  final _pinCtrl = TextEditingController();

  @override
  void dispose() {
    _telCtrl.dispose();
    _otpCtrl.dispose();
    _pinCtrl.dispose();
    super.dispose();
  }

  Future<void> _envoyerCode() async {
    if (!_formInitierKey.currentState!.validate()) return;
    await ref.read(agentEnrolementProvider.notifier).initierEnrolement(_telCtrl.text.trim(), _typeActeur);
    _telCtrl.clear();
  }

  Future<void> _activer() async {
    if (!_formActiverKey.currentState!.validate()) return;
    final enrolement = ref.read(agentEnrolementProvider).dernierEnrolement;
    if (enrolement == null) return;
    final succes = await ref
        .read(agentEnrolementProvider.notifier)
        .activerEnrolement(enrolement.enrolementId, _otpCtrl.text.trim(), _pinCtrl.text.trim());
    if (succes) {
      _otpCtrl.clear();
      _pinCtrl.clear();
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(agentEnrolementProvider);
    final enrolement = state.dernierEnrolement;
    final enAttenteActivation = enrolement != null && enrolement.statut == 'EN_ATTENTE';
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(
        title: Text(t.enrolerUnChauffeur),
        actions: [
          if (state.fileAttente.isNotEmpty)
            IconButton(
              icon: const Icon(Icons.sync, color: AppColors.accent),
              tooltip: t.synchroniserFileOffline,
              onPressed: () => ref.read(agentEnrolementProvider.notifier).synchroniserFileAttente(),
            ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (state.fileAttente.isNotEmpty) _bandeauFileAttente(t, state.fileAttente.length),
              if (state.succes != null) _bandeau(state.succes!, AppColors.succes, Icons.check_circle),
              if (state.erreur != null) _bandeau(state.erreur!, AppColors.erreur, Icons.warning_amber),
              const SizedBox(height: 12),

              if (!enAttenteActivation) _formulaireInitier(t, state) else _formulaireActiver(t, state, enrolement),
            ],
          ),
        ),
      ),
    );
  }

  Widget _bandeauFileAttente(AppLocalizations t, int nombre) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.accent.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.accent.withValues(alpha: 0.4)),
      ),
      child: Row(children: [
        const Icon(Icons.cloud_off, color: AppColors.accent, size: 18),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            t.enrolementsEnAttenteSync(nombre),
            style: const TextStyle(color: AppColors.accent, fontSize: 13),
          ),
        ),
      ]),
    );
  }

  Widget _bandeau(String message, Color couleur, IconData icone) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: couleur.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: couleur.withValues(alpha: 0.4)),
      ),
      child: Row(children: [
        Icon(icone, color: couleur, size: 18),
        const SizedBox(width: 8),
        Expanded(child: Text(message, style: TextStyle(color: couleur, fontSize: 13))),
      ]),
    );
  }

  Widget _formulaireInitier(AppLocalizations t, AgentEnrolementState state) {
    return Form(
      key: _formInitierKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(t.nouvelEnrolementTitre, style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 4),
          Text(t.codeActivationSmsMessage,
              style: const TextStyle(fontSize: 13, color: AppColors.texteMuet)),
          const SizedBox(height: 20),

          Text(t.typeSectionLabel, style: const TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            initialValue: _typeActeur,
            decoration: InputDecoration(
              filled: true,
              fillColor: AppColors.surface,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
            ),
            items: _typesActeurEnrolables(t).entries
                .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value)))
                .toList(),
            onChanged: (v) => setState(() => _typeActeur = v!),
          ),
          const SizedBox(height: 16),

          Text(t.telephoneDeLaPersonneLabel, style: const TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          TextFormField(
            controller: _telCtrl,
            keyboardType: TextInputType.phone,
            style: const TextStyle(color: AppColors.texte, fontSize: 15),
            decoration: InputDecoration(
              hintText: '+237 6XX XXX XXX',
              filled: true,
              fillColor: AppColors.surface,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
              prefixIcon: const Icon(Icons.phone, color: AppColors.texteMuet),
            ),
            validator: (v) {
              if (v == null || v.isEmpty) return t.telephoneObligatoire;
              if (!RegExp(r'^\+?[0-9]{9,15}$').hasMatch(v)) return t.formatInvalide;
              return null;
            },
          ),
          const SizedBox(height: 20),

          SizedBox(
            width: double.infinity,
            height: 52,
            child: ElevatedButton.icon(
              onPressed: state.chargement ? null : _envoyerCode,
              icon: state.chargement
                  ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                  : const Icon(Icons.sms_outlined, color: AppColors.texteBouton),
              label: Text(state.chargement ? t.envoiEnCours : t.envoyerLeCode,
                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _formulaireActiver(AppLocalizations t, AgentEnrolementState state, EnrolementActif enrolement) {
    return Form(
      key: _formActiverKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(t.activerLeCompte, style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 4),
          Text('${enrolement.telephone} — ${_typesActeurEnrolables(t)[enrolement.typeActeur] ?? enrolement.typeActeur}',
              style: const TextStyle(color: AppColors.texteMuet, fontSize: 13)),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppColors.bordure),
            ),
            child: Row(children: [
              const Icon(Icons.info_outline, color: AppColors.accentProfond, size: 18),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  t.codeEtPinParLaPersonneMessage,
                  style: const TextStyle(color: AppColors.texteMuet, fontSize: 12),
                ),
              ),
            ]),
          ),
          const SizedBox(height: 20),

          Text(t.codeRecuParSmsLabel, style: const TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          TextFormField(
            controller: _otpCtrl,
            keyboardType: TextInputType.number,
            maxLength: 6,
            style: const TextStyle(color: AppColors.texte, fontSize: 20, letterSpacing: 6),
            decoration: InputDecoration(
              hintText: '••••••',
              counterText: '',
              filled: true,
              fillColor: AppColors.surface,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
            ),
            validator: (v) => (v == null || v.length != 6) ? t.codeSixChiffres : null,
          ),
          const SizedBox(height: 16),

          Text(t.nouveauCodePinLabel, style: const TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          TextFormField(
            controller: _pinCtrl,
            obscureText: true,
            keyboardType: TextInputType.number,
            maxLength: 6,
            style: const TextStyle(color: AppColors.texte, fontSize: 20, letterSpacing: 6),
            decoration: InputDecoration(
              hintText: '••••',
              counterText: '',
              filled: true,
              fillColor: AppColors.surface,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
            ),
            validator: (v) {
              if (v == null || v.isEmpty) return t.pinObligatoire;
              if (!RegExp(r'^[0-9]{4,6}$').hasMatch(v)) return t.codeFormatInvalide;
              return null;
            },
          ),
          const SizedBox(height: 20),

          SizedBox(
            width: double.infinity,
            height: 52,
            child: ElevatedButton(
              onPressed: state.chargement ? null : _activer,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.succes,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: state.chargement
                  ? const SizedBox(height: 22, width: 22, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                  : Text(t.activerLeCompte,
                      style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
            ),
          ),
          const SizedBox(height: 12),
          TextButton(
            onPressed: () => ref.read(agentEnrolementProvider.notifier).reinitialiser(),
            child: Text(t.nouvelEnrolementTitre, style: const TextStyle(color: AppColors.texteMuet)),
          ),
        ],
      ),
    );
  }
}
