import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/litige_provider.dart';
import '../theme/app_theme.dart';

/// S19 (Sprint 19, "Back-office avancé, litiges"), Volet Client — appel réel
/// depuis le 23 août, voir litige_provider.dart. Signalement d'un litige en
/// cohérence avec le contexte d'une mission donnée (motif, description,
/// référence mission), accessible depuis l'écran de suivi (suivi_screen.dart).
class LitigeScreen extends ConsumerStatefulWidget {
  final String demandeId;
  final String missionId;

  const LitigeScreen({super.key, required this.demandeId, required this.missionId});

  @override
  ConsumerState<LitigeScreen> createState() => _LitigeScreenState();
}

class _LitigeScreenState extends ConsumerState<LitigeScreen> {
  final _descriptionCtrl = TextEditingController();
  String _motif = motifsLitige.first;

  @override
  void dispose() {
    _descriptionCtrl.dispose();
    super.dispose();
  }

  Future<void> _envoyer() async {
    await ref.read(litigeProvider.notifier).envoyer(
          demandeId: widget.demandeId,
          missionId: widget.missionId,
          motif: _motif,
          description: _descriptionCtrl.text.trim(),
        );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(litigeProvider);
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.signalerLitige)),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (state.erreur != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.erreur.withValues(alpha: 0.08),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: AppColors.erreur.withValues(alpha: 0.4)),
                ),
                child: Row(children: [
                  const Icon(Icons.error_outline, color: AppColors.erreur, size: 18),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(state.erreur!, style: const TextStyle(color: AppColors.erreur, fontSize: 12)),
                  ),
                ]),
              ),
              const SizedBox(height: 16),
            ],
            if (state.envoye)
              _confirmation()
            else ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: AppColors.surfaceClaire, borderRadius: BorderRadius.circular(10)),
                child: Text(t.missionConcernee(widget.missionId),
                    style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
              ),
              const SizedBox(height: 20),
              Text(t.motif, style: const TextStyle(fontSize: 11, letterSpacing: 1.1,
                  color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              // motifsLitige reste en français quelle que soit la langue de
              // l'app : c'est un texte libre ENVOYE TEL QUEL au Bureau
              // (service-adm, cf litige_provider.dart) - traduire l'affichage
              // changerait ce qui est reellement transmis au destinataire.
              DropdownButtonFormField<String>(
                initialValue: _motif,
                decoration: InputDecoration(
                  filled: true,
                  fillColor: AppColors.surface,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
                ),
                items: motifsLitige.map((m) => DropdownMenuItem(value: m, child: Text(m))).toList(),
                onChanged: (v) => setState(() => _motif = v!),
              ),
              const SizedBox(height: 16),
              Text(t.description, style: const TextStyle(fontSize: 11, letterSpacing: 1.1,
                  color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              TextField(
                controller: _descriptionCtrl,
                maxLines: 4,
                decoration: InputDecoration(
                  hintText: t.hintDescriptionLitige,
                  filled: true,
                  fillColor: AppColors.surface,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.bordure)),
                ),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton(
                  onPressed: state.envoiEnCours ? null : _envoyer,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.accent,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  ),
                  child: state.envoiEnCours
                      ? const SizedBox(height: 22, width: 22,
                          child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                      : Text(t.envoyerSignalement,
                          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _confirmation() {
    final t = AppLocalizations.of(context);
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.succes.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.succes.withValues(alpha: 0.4)),
      ),
      child: Row(children: [
        const Icon(Icons.check_circle, color: AppColors.succes),
        const SizedBox(width: 10),
        Expanded(
          child: Text(t.litigeConfirmation,
              style: const TextStyle(color: AppColors.succes)),
        ),
      ]),
    );
  }
}
