import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: const Text('Signaler un litige')),
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
                child: Text('Mission concernée : ${widget.missionId}',
                    style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
              ),
              const SizedBox(height: 20),
              const Text('MOTIF', style: TextStyle(fontSize: 11, letterSpacing: 1.1,
                  color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
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
              const Text('DESCRIPTION', style: TextStyle(fontSize: 11, letterSpacing: 1.1,
                  color: AppColors.texteMuet, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              TextField(
                controller: _descriptionCtrl,
                maxLines: 4,
                decoration: InputDecoration(
                  hintText: 'Décrivez le problème rencontré',
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
                      : const Text('Envoyer le signalement',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _confirmation() {
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
        const Expanded(
          child: Text('Votre signalement a été transmis. Le Bureau reviendra vers vous.',
              style: TextStyle(color: AppColors.succes)),
        ),
      ]),
    );
  }
}
