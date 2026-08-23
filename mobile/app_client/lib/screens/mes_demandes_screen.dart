import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/demande_provider.dart';
import '../models/demande_model.dart';
import '../theme/app_theme.dart';
import '../widgets/top_notification.dart';
import 'publier_demande_screen.dart';
import 'propositions_screen.dart';
import 'suivi_screen.dart';

class MesDemandesScreen extends ConsumerWidget {
  const MesDemandesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final demandeState = ref.watch(demandeProvider);
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(
        title: Text(t.mesDemandes),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: AppColors.texteMuet),
            onPressed: () => ref.read(demandeProvider.notifier).chargerMesDemandes(),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppColors.accent,
        onPressed: () async {
          await Navigator.push(context, MaterialPageRoute(builder: (_) => const PublierDemandeScreen()));
          ref.read(demandeProvider.notifier).chargerMesDemandes();
        },
        icon: const Icon(Icons.add, color: Colors.white),
        label: Text(t.nouvelleDemande, style: const TextStyle(color: Colors.white)),
      ),
      body: demandeState.chargement
          ? const Center(child: CircularProgressIndicator(color: AppColors.accent))
          : demandeState.mesDemandes.isEmpty
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Text(t.aucuneDemande,
                        style: const TextStyle(color: AppColors.texteMuet), textAlign: TextAlign.center),
                  ),
                )
              : RefreshIndicator(
                  color: AppColors.accent,
                  onRefresh: () => ref.read(demandeProvider.notifier).chargerMesDemandes(),
                  child: Builder(builder: (context) {
                    // Ordre chronologique croissant (la plus ancienne en
                    // premier) plutôt que chaque nouvelle demande empilée
                    // au-dessus des précédentes : le moteur sert toujours la
                    // plus ancienne demande PUBLIEE en attente en premier
                    // (FIFO par capacité disponible) — la position affichée
                    // reflète directement cet ordre de service réel plutôt
                    // que l'ordre de création.
                    final triees = [...demandeState.mesDemandes]
                      ..sort((a, b) => a.dateCreation.compareTo(b.dateCreation));
                    var rang = 0;
                    return ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: triees.length,
                      itemBuilder: (context, i) {
                        final d = triees[i];
                        final positionFile = d.statut == 'PUBLIEE' ? ++rang : null;
                        return _DemandeCard(demande: d, positionFile: positionFile);
                      },
                    );
                  }),
                ),
    );
  }
}

class _DemandeCard extends ConsumerWidget {
  final DemandeModel demande;
  final int? positionFile;
  const _DemandeCard({required this.demande, this.positionFile});

  Future<void> _annuler(BuildContext context, WidgetRef ref) async {
    final t = AppLocalizations.of(context);
    final confirme = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.annulerCetteDemandeTitre),
        content: Text(t.annulerCetteDemandeContenu),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: Text(t.retour)),
          TextButton(onPressed: () => Navigator.pop(context, true), child: Text(t.annulerLaDemande, style: const TextStyle(color: AppColors.erreur))),
        ],
      ),
    );
    if (confirme != true || !context.mounted) return;
    final erreur = await ref.read(demandeProvider.notifier).annulerDemande(demande.id);
    if (!context.mounted) return;
    if (erreur != null) {
      afficherNotification(context, message: erreur, couleur: AppColors.erreur, icone: Icons.error_outline);
    } else {
      afficherNotification(context, message: t.demandeAnnulee, couleur: AppColors.succes, icone: Icons.check_circle);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = AppLocalizations.of(context);
    return InkWell(
      onTap: () => Navigator.push(
        context,
        MaterialPageRoute(builder: (_) => PropositionsScreen(demande: demande)),
      ),
      borderRadius: BorderRadius.circular(12),
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.bordure),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (positionFile != null) ...[
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: positionFile == 1 ? AppColors.succes.withValues(alpha: 0.12) : AppColors.surfaceClaire,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  positionFile == 1 ? t.prochaineAEtreServie : t.positionDansLaFile(positionFile!),
                  style: TextStyle(
                    fontSize: 10, fontWeight: FontWeight.bold,
                    color: positionFile == 1 ? AppColors.succes : AppColors.texteMuet,
                  ),
                ),
              ),
              const SizedBox(height: 6),
            ],
            Row(children: [
              const Icon(Icons.trip_origin, size: 14, color: AppColors.texteMuet),
              Text(' ${demande.villeDepart} ', style: const TextStyle(fontSize: 13)),
              const Icon(Icons.arrow_forward, size: 12, color: AppColors.texteMuet),
              const Icon(Icons.place, size: 14, color: AppColors.texteMuet),
              Text(' ${demande.villeArrivee}', style: const TextStyle(fontSize: 13)),
            ]),
            const SizedBox(height: 6),
            Text('${demande.typeEmballageNom} × ${demande.quantite} — ${demande.poidsTotalKg.toStringAsFixed(0)} kg',
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
            const SizedBox(height: 4),
            Text(
              '${_libelleDisponibilite(t, demande.typeDisponibilite)} · ${_libelleCollecte(t, demande.modeCollecte)}',
              style: const TextStyle(color: AppColors.texteMuet, fontSize: 12),
            ),
            const SizedBox(height: 2),
            Text(
              t.destinataireLabel(demande.destinataireNom, demande.destinataireTelephone),
              style: const TextStyle(color: AppColors.texteMuet, fontSize: 12),
            ),
            const SizedBox(height: 2),
            Text(
              t.publieeLe(_formatDate(demande.dateCreation)),
              style: const TextStyle(color: AppColors.texteMuet, fontSize: 11),
            ),
            if (demande.fragile || demande.perissable || demande.dangereuse || demande.grandeValeur) ...[
              const SizedBox(height: 6),
              Wrap(spacing: 6, runSpacing: 4, children: [
                if (demande.fragile) _badge(t.fragile),
                if (demande.perissable) _badge(t.perissable),
                if (demande.dangereuse) _badge(t.dangereuse),
                if (demande.grandeValeur) _badge(t.grandeValeur),
              ]),
            ],
            const SizedBox(height: 6),
            Row(children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: AppColors.surfaceClaire,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(demande.statut, style: const TextStyle(fontSize: 10, color: AppColors.accent, fontWeight: FontWeight.bold)),
              ),
              const Spacer(),
              Flexible(
                child: Text(t.voirLesPropositions, style: const TextStyle(fontSize: 11, color: AppColors.texteMuet), overflow: TextOverflow.ellipsis),
              ),
              const Icon(Icons.chevron_right, size: 16, color: AppColors.texteMuet),
            ]),
            const SizedBox(height: 8),
            // Actions sur une ligne séparée, avec retour à la ligne
            // automatique (Wrap) plutôt qu'un unique Row qui débordait du
            // cadre de la carte une fois Modifier/Annuler ajoutés à côté
            // de Suivi.
            Wrap(
              spacing: 14,
              runSpacing: 4,
              children: [
                TextButton.icon(
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => SuiviScreen(demandeId: demande.id, demande: demande)),
                  ),
                  icon: const Icon(Icons.location_on_outlined, size: 14, color: AppColors.accent),
                  label: Text(t.suivi, style: const TextStyle(fontSize: 11, color: AppColors.accent)),
                  style: TextButton.styleFrom(padding: EdgeInsets.zero, minimumSize: const Size(0, 0)),
                ),
                if (demande.statut == 'PUBLIEE') ...[
                  TextButton.icon(
                    onPressed: () async {
                      await Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => PublierDemandeScreen(demandeAModifier: demande)),
                      );
                      if (context.mounted) ref.read(demandeProvider.notifier).chargerMesDemandes();
                    },
                    icon: const Icon(Icons.edit_outlined, size: 14, color: AppColors.accent),
                    label: Text(t.modifier, style: const TextStyle(fontSize: 11, color: AppColors.accent)),
                    style: TextButton.styleFrom(padding: EdgeInsets.zero, minimumSize: const Size(0, 0)),
                  ),
                  TextButton.icon(
                    onPressed: () => _annuler(context, ref),
                    icon: const Icon(Icons.delete_outline, size: 14, color: AppColors.erreur),
                    label: Text(t.annuler, style: const TextStyle(fontSize: 11, color: AppColors.erreur)),
                    style: TextButton.styleFrom(padding: EdgeInsets.zero, minimumSize: const Size(0, 0)),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _badge(String texte) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
        decoration: BoxDecoration(
          color: AppColors.erreur.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(texte, style: const TextStyle(fontSize: 10, color: AppColors.erreur, fontWeight: FontWeight.bold)),
      );

  String _libelleDisponibilite(AppLocalizations t, String v) {
    switch (v) {
      case 'DES_QUE_POSSIBLE': return t.desQuePossible;
      case 'DATE_PRECISE': return t.datePrecise;
      case 'PLAGE': return t.plageHoraire;
      default: return v;
    }
  }

  String _libelleCollecte(AppLocalizations t, String v) {
    switch (v) {
      case 'DOMICILE': return t.collecteADomicile;
      case 'POINT_RELAIS': return t.pointRelais;
      default: return v;
    }
  }

  String _formatDate(DateTime d) {
    final heure = d.hour.toString().padLeft(2, '0');
    final minute = d.minute.toString().padLeft(2, '0');
    return '${d.day}/${d.month}/${d.year} à $heure:$minute';
  }
}
