import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/agent_enrolement_provider.dart';
import '../theme/app_theme.dart';

const _typesActeurEnrolables = {
  'CHAUFFEUR': 'Chauffeur',
  'TRANSPORTEUR': 'Transporteur',
  'CHAUFFEUR_PROPRIETAIRE': 'Chauffeur-propriétaire',
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

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(
        title: const Text('Enrôler un chauffeur'),
        actions: [
          if (state.fileAttente.isNotEmpty)
            IconButton(
              icon: const Icon(Icons.sync, color: AppColors.accent),
              tooltip: 'Synchroniser la file offline',
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
              if (state.fileAttente.isNotEmpty) _bandeauFileAttente(state.fileAttente.length),
              if (state.succes != null) _bandeau(state.succes!, AppColors.succes, Icons.check_circle),
              if (state.erreur != null) _bandeau(state.erreur!, AppColors.erreur, Icons.warning_amber),
              const SizedBox(height: 12),

              if (!enAttenteActivation) _formulaireInitier(state) else _formulaireActiver(state, enrolement),
            ],
          ),
        ),
      ),
    );
  }

  Widget _bandeauFileAttente(int nombre) {
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
            '$nombre enrôlement${nombre > 1 ? 's' : ''} en attente de synchronisation (hors ligne).',
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

  Widget _formulaireInitier(AgentEnrolementState state) {
    return Form(
      key: _formInitierKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Nouvel enrôlement', style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 4),
          const Text('Le code d\'activation part par SMS directement au téléphone de la personne — jamais au vôtre.',
              style: TextStyle(fontSize: 13, color: AppColors.texteMuet)),
          const SizedBox(height: 20),

          const Text('TYPE', style: TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            initialValue: _typeActeur,
            decoration: InputDecoration(
              filled: true,
              fillColor: AppColors.surface,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
            ),
            items: _typesActeurEnrolables.entries
                .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value)))
                .toList(),
            onChanged: (v) => setState(() => _typeActeur = v!),
          ),
          const SizedBox(height: 16),

          const Text('TÉLÉPHONE DE LA PERSONNE', style: TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
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
              if (v == null || v.isEmpty) return 'Téléphone obligatoire';
              if (!RegExp(r'^\+?[0-9]{9,15}$').hasMatch(v)) return 'Format invalide';
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
              label: Text(state.chargement ? 'Envoi…' : 'Envoyer le code',
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

  Widget _formulaireActiver(AgentEnrolementState state, EnrolementActif enrolement) {
    return Form(
      key: _formActiverKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Activer le compte', style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 4),
          Text('${enrolement.telephone} — ${_typesActeurEnrolables[enrolement.typeActeur] ?? enrolement.typeActeur}',
              style: const TextStyle(color: AppColors.texteMuet, fontSize: 13)),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppColors.bordure),
            ),
            child: const Row(children: [
              Icon(Icons.info_outline, color: AppColors.accentProfond, size: 18),
              SizedBox(width: 8),
              Expanded(
                child: Text(
                  'À faire saisir par la personne elle-même : le code reçu par SMS, puis un code PIN de son choix.',
                  style: TextStyle(color: AppColors.texteMuet, fontSize: 12),
                ),
              ),
            ]),
          ),
          const SizedBox(height: 20),

          const Text('CODE REÇU PAR SMS', style: TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
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
            validator: (v) => (v == null || v.length != 6) ? 'Code à 6 chiffres' : null,
          ),
          const SizedBox(height: 16),

          const Text('NOUVEAU CODE PIN (4-6 chiffres)', style: TextStyle(fontSize: 11, letterSpacing: 1.2, color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
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
              if (v == null || v.isEmpty) return 'PIN obligatoire';
              if (!RegExp(r'^[0-9]{4,6}$').hasMatch(v)) return '4 à 6 chiffres';
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
                  : const Text('Activer le compte',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
            ),
          ),
          const SizedBox(height: 12),
          TextButton(
            onPressed: () => ref.read(agentEnrolementProvider.notifier).reinitialiser(),
            child: const Text('Nouvel enrôlement', style: TextStyle(color: AppColors.texteMuet)),
          ),
        ],
      ),
    );
  }
}
