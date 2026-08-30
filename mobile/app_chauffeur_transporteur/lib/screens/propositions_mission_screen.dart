import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/proposition_mission_provider.dart';
import '../theme/app_theme.dart';

// UC-MAT-02/diffusion-course (30/08, ADR 0019) : rémunération affichée en
// premier (RG-049), origine/destination/distance/durée, compte à rebours
// avant expiration (15 min par défaut, cf service-opt) ; en cas de refus,
// liste courte de motifs sans champ libre obligatoire (RG-050/RG-051, aucune
// pénalité de fiabilité pour un refus isolé). Sous ce modèle, la même
// demande peut être diffusée à plusieurs chauffeurs en parallèle -- premier
// acceptant gagne, une proposition perdue disparaît toute seule au prochain
// rafraîchissement (statut != PROPOSEE).
class PropositionsMissionScreen extends ConsumerStatefulWidget {
  const PropositionsMissionScreen({super.key});

  @override
  ConsumerState<PropositionsMissionScreen> createState() => _PropositionsMissionScreenState();
}

class _PropositionsMissionScreenState extends ConsumerState<PropositionsMissionScreen> {
  Timer? _tick;

  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(propositionMissionProvider.notifier).charger());
    // Rafraîchit l'affichage du compte à rebours chaque seconde ; recharge
    // la liste toutes les 15s pour faire apparaître de nouvelles propositions.
    var compteur = 0;
    _tick = Timer.periodic(const Duration(seconds: 1), (_) {
      compteur++;
      if (compteur % 15 == 0) {
        ref.read(propositionMissionProvider.notifier).charger();
      } else if (mounted) {
        setState(() {});
      }
    });
  }

  @override
  void dispose() {
    _tick?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(propositionMissionProvider);
    final enAttente = state.enAttente;
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.mesPropositionsMission)),
      body: RefreshIndicator(
        onRefresh: () => ref.read(propositionMissionProvider.notifier).charger(),
        child: state.chargement && enAttente.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null && enAttente.isEmpty
                ? _erreur(state.erreur!)
                : enAttente.isEmpty
                    ? ListView(children: [
                        const SizedBox(height: 80),
                        Center(child: Text(t.aucunePropositionMission, style: const TextStyle(color: AppColors.texteMuet))),
                      ])
                    : ListView(
                        padding: const EdgeInsets.all(16),
                        children: [
                          for (final p in enAttente) ...[
                            _cartePropositionMission(t, p),
                            const SizedBox(height: 12),
                          ],
                        ],
                      ),
      ),
    );
  }

  Widget _erreur(String message) {
    return ListView(children: [
      const SizedBox(height: 80),
      const Center(child: Icon(Icons.wifi_off, color: AppColors.texteMuet, size: 48)),
      const SizedBox(height: 12),
      Center(child: Text(message, style: const TextStyle(color: AppColors.texteMuet))),
    ]);
  }

  Widget _cartePropositionMission(AppLocalizations t, PropositionMission p) {
    final restant = p.expireA?.difference(DateTime.now());
    final expiree = restant != null && restant.isNegative;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.accent.withValues(alpha: 0.35)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(t.remunerationLabel, style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                  Text(
                    p.prixTransport != null ? '${p.prixTransport!.toStringAsFixed(0)} XAF' : '—',
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 22, color: AppColors.accent),
                  ),
                ],
              ),
              if (!expiree && restant != null)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(color: AppColors.surfaceClaire, borderRadius: BorderRadius.circular(20)),
                  child: Text(
                    t.expireDans(restant.inSeconds.clamp(0, 999999).toString()),
                    style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.accentProfond),
                  ),
                )
              else
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(color: AppColors.bordure, borderRadius: BorderRadius.circular(20)),
                  child: Text(t.propositionExpiree, style: const TextStyle(fontSize: 12, color: AppColors.texteMuet)),
                ),
            ],
          ),
          const Divider(height: 24),
          _ligne(Icons.trip_origin, p.origineNom ?? '—'),
          const Padding(
            padding: EdgeInsets.only(left: 11),
            child: SizedBox(height: 18, child: VerticalDivider(width: 1, thickness: 2)),
          ),
          _ligne(Icons.location_on, p.destinationNom ?? '—'),
          const SizedBox(height: 10),
          // Marchandise/poids/destinataire pas encore exposes par le GET
          // OPT (AffectationResponse) -- distance/duree seulement pour
          // l'instant, cf plan-fretcorridor-reorientation.md.
          Wrap(spacing: 16, runSpacing: 6, children: [
            if (p.distanceMetres != null) _detail(t.distanceLabel, '${(p.distanceMetres! / 1000).toStringAsFixed(0)} km'),
            if (p.dureeSecondes != null) _detail(t.dureeLabel, '${(p.dureeSecondes! / 60).round()} min'),
          ]),
          const SizedBox(height: 16),
          Row(children: [
            Expanded(
              child: OutlinedButton(
                onPressed: expiree ? null : () => _ouvrirMotifsRefus(t, p),
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: AppColors.erreur),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
                child: Text(t.refuser, style: const TextStyle(color: AppColors.erreur)),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: ElevatedButton(
                onPressed: expiree ? null : () => _accepter(t, p),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.accent,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                ),
                child: Text(t.accepter, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.texteBouton)),
              ),
            ),
          ]),
        ],
      ),
    );
  }

  Widget _ligne(IconData icone, String texte) {
    return Row(children: [
      Icon(icone, size: 16, color: AppColors.accent),
      const SizedBox(width: 8),
      Expanded(child: Text(texte, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13))),
    ]);
  }

  Widget _detail(String label, String valeur) {
    return SizedBox(
      width: 150,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(color: AppColors.texteMuet, fontSize: 11)),
          Text(valeur, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }

  Future<void> _accepter(AppLocalizations t, PropositionMission p) async {
    final erreur = await ref.read(propositionMissionProvider.notifier).accepter(p);
    if (!mounted) return;
    _afficherResultat(erreur == null ? t.missionAcceptee : t.propositionIndisponible, succes: erreur == null);
  }

  // Le motif choisi n'est pas encore transmis au backend (RG-050 demande la
  // question a l'utilisateur, mais le contrat demande-refusee-par-chauffeur.yaml
  // ne porte aucun champ motif a ce jour) -- on garde la question cote UX,
  // simplement sans persistance serveur pour l'instant.
  Future<void> _ouvrirMotifsRefus(AppLocalizations t, PropositionMission p) async {
    final motif = await showModalBottomSheet<String>(
      context: context,
      backgroundColor: AppColors.fond,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (context) => _FeuilleMotifsRefus(t: t),
    );
    if (motif == null || !mounted) return;
    final erreur = await ref.read(propositionMissionProvider.notifier).refuser(p);
    if (!mounted) return;
    _afficherResultat(erreur == null ? t.missionRefusee : t.propositionIndisponible, succes: erreur == null);
  }

  void _afficherResultat(String message, {required bool succes}) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(message),
      backgroundColor: succes ? AppColors.succes : AppColors.erreur,
    ));
  }
}

class _FeuilleMotifsRefus extends StatelessWidget {
  final AppLocalizations t;

  const _FeuilleMotifsRefus({required this.t});

  @override
  Widget build(BuildContext context) {
    final motifs = [t.motifTropLoin, t.motifIndisponible, t.motifRemunerationInsuffisante, t.motifVehiculeInadapte, t.motifAutre];
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(t.motifRefusTitre, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            const SizedBox(height: 16),
            for (final motif in motifs)
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(motif),
                trailing: const Icon(Icons.chevron_right, color: AppColors.texteMuet),
                onTap: () => Navigator.pop(context, motif),
              ),
          ],
        ),
      ),
    );
  }
}
