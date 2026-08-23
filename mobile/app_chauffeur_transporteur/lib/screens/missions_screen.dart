import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../l10n/app_localizations.dart';
import '../providers/mission_provider.dart';
import '../theme/app_theme.dart';
import 'mission_detail_screen.dart';
import 'mission_multi_etapes_screen.dart';

Map<String, String> _libellesStatut(AppLocalizations t) => {
      'EN_ATTENTE': t.statutEnAttente,
      'PRISE_EN_CHARGE': t.statutPriseEnCharge,
      'EN_TRANSIT': t.statutEnTransit,
      'LIVREE': t.statutLivree,
      'ANNULEE': t.statutAnnulee,
    };

Map<String, String> _libellesDisponibilite(AppLocalizations t) => {
      'DES_QUE_POSSIBLE': t.disponibiliteDesQuePossible,
      'DATE_PRECISE': t.disponibiliteDatePrecise,
      'PLAGE': t.disponibilitePlage,
    };

Map<String, String> _libellesCollecte(AppLocalizations t) => {
      'DOMICILE': t.collecteDomicile,
      'POINT_RELAIS': t.collectePointRelais,
    };

// Identifiant lisible affiche a l'ecran (mockup "Mes missions") : aucune
// numerotation sequentielle n'existe cote backend, on derive un code stable
// et lisible depuis missionId + date plutot que d'exposer l'UUID brut.
String _idAffiche(String missionId, String? dateCreationIso) {
  String datePart = '';
  if (dateCreationIso != null) {
    final d = DateTime.tryParse(dateCreationIso);
    if (d != null) {
      datePart = '${d.year}${d.month.toString().padLeft(2, '0')}${d.day.toString().padLeft(2, '0')}-';
    }
  }
  return '#MIS-$datePart${missionId.substring(0, 8).toUpperCase()}';
}

// S7 : liste des missions du chauffeur/transporteur connecté. Peut rester
// vide tant que le lien mission↔chauffeur n'est pas peuplé en amont côté
// service-opt (écart documenté, cf. README) — pas un bug de cet écran.
class MissionsScreen extends ConsumerStatefulWidget {
  const MissionsScreen({super.key});

  @override
  ConsumerState<MissionsScreen> createState() => _MissionsScreenState();
}

class _MissionsScreenState extends ConsumerState<MissionsScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(missionProvider.notifier).chargerMesMissions());
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(missionProvider);
    final t = AppLocalizations.of(context);

    return Scaffold(
      backgroundColor: AppColors.fond,
      appBar: AppBar(title: Text(t.mesMissions)),
      body: RefreshIndicator(
        onRefresh: () => ref.read(missionProvider.notifier).chargerMesMissions(),
        child: state.chargement && state.missions.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : state.erreur != null
                ? _erreur(state.erreur!)
                : state.missions.isEmpty
                    ? ListView(children: [
                        const SizedBox(height: 80),
                        Center(
                          child: Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 32),
                            child: Text(
                              t.aucuneMissionPourLeMoment,
                              textAlign: TextAlign.center,
                              style: const TextStyle(color: AppColors.texteMuet),
                            ),
                          ),
                        ),
                      ])
                    : ListView.separated(
                        padding: const EdgeInsets.all(16),
                        itemCount: state.missions.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 10),
                        itemBuilder: (context, i) => _carteMission(t, state.missions[i]),
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

  Widget _carteMission(AppLocalizations t, Mission mission) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.bordure),
      ),
      child: Column(
        children: [
          InkWell(
            onTap: () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => MissionDetailScreen(mission: mission)));
            },
            borderRadius: BorderRadius.circular(12),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    Container(
                      width: 44, height: 44,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(color: AppColors.surfaceClaire, borderRadius: BorderRadius.circular(10)),
                      child: const Icon(Icons.local_shipping_outlined, color: AppColors.accent),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('${mission.origineNom ?? '—'} → ${mission.destinationNom ?? '—'}',
                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                          const SizedBox(height: 4),
                          Wrap(crossAxisAlignment: WrapCrossAlignment.center, spacing: 6, children: [
                            Text(t.idDeLaMission, style: const TextStyle(fontSize: 11, color: AppColors.texteMuet)),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(color: AppColors.surfaceClaire, borderRadius: BorderRadius.circular(6)),
                              child: Text(_idAffiche(mission.missionId, mission.dateCreation),
                                  style: const TextStyle(fontSize: 11, color: AppColors.accent, fontWeight: FontWeight.bold)),
                            ),
                          ]),
                        ],
                      ),
                    ),
                    const Icon(Icons.chevron_right, color: AppColors.texteMuet),
                  ]),
                  const Divider(height: 24),
                  if (mission.typeEmballageNom != null)
                    _ligneMission(Icons.inventory_2_outlined,
                        '${mission.quantite ?? ''} × ${mission.typeEmballageNom}'
                        '${mission.poidsTaxableKg != null ? ' — ${mission.poidsTaxableKg!.toStringAsFixed(0)} kg' : ''}',
                        gras: true),
                  if (mission.typeDisponibilite != null || mission.demandeModeCollecte != null)
                    _ligneMission(Icons.calendar_today_outlined,
                        [
                          if (mission.typeDisponibilite != null) _libellesDisponibilite(t)[mission.typeDisponibilite] ?? mission.typeDisponibilite!,
                          if (mission.demandeModeCollecte != null) _libellesCollecte(t)[mission.demandeModeCollecte] ?? mission.demandeModeCollecte!,
                        ].join(' · ')),
                  if (mission.destinataireNom != null)
                    _ligneMission(Icons.person_outline,
                        mission.destinataireTelephone != null
                            ? t.destinataireAvecTel(mission.destinataireNom!, mission.destinataireTelephone!)
                            : t.destinataireSansTel(mission.destinataireNom!)),
                  if (mission.poidsTotalKg != null)
                    _ligneMission(Icons.shopping_bag_outlined, t.poidsTotalLabel(mission.poidsTotalKg!.toStringAsFixed(0))),
                  if (mission.typeEmballageNom != null)
                    _ligneMission(Icons.category_outlined, t.typeLabel(mission.typeEmballageNom!)),
                  if (mission.dateCreation != null)
                    _ligneMission(Icons.access_time, t.publieeLe(_dateAffichee(mission.dateCreation!))),
                  Text(_libellesStatut(t)[mission.statut] ?? mission.statut,
                      style: const TextStyle(color: AppColors.texteMuet, fontSize: 12)),
                  if (mission.grandeValeur == true) ...[
                    const SizedBox(height: 8),
                    Row(children: [
                      const Icon(Icons.star_border, color: AppColors.accent, size: 16),
                      const SizedBox(width: 8),
                      Text(t.valeurLabel, style: const TextStyle(fontSize: 13)),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                        decoration: BoxDecoration(color: AppColors.surfaceClaire, borderRadius: BorderRadius.circular(20)),
                        child: Text(t.grandeValeur,
                            style: const TextStyle(color: AppColors.accent, fontSize: 11, fontWeight: FontWeight.bold)),
                      ),
                    ]),
                  ],
                ],
              ),
            ),
          ),
          // S11 (EF-MAT-05/06) : présent uniquement si service-opt a
          // regroupé cette Mission dans une Tournée consolidée (LTL).
          if (mission.tourneeId != null)
            InkWell(
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => MissionMultiEtapesScreen(tourneeId: mission.tourneeId!)),
                );
              },
              borderRadius: const BorderRadius.vertical(bottom: Radius.circular(12)),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                decoration: BoxDecoration(
                  color: AppColors.accent.withValues(alpha: 0.08),
                  borderRadius: const BorderRadius.vertical(bottom: Radius.circular(12)),
                  border: Border(top: BorderSide(color: AppColors.bordure)),
                ),
                child: Row(children: [
                  const Icon(Icons.alt_route_outlined, color: AppColors.accent, size: 16),
                  const SizedBox(width: 8),
                  Text(t.faitPartieTourneeGroupee, style: const TextStyle(color: AppColors.accent, fontSize: 12)),
                  const Spacer(),
                  const Icon(Icons.chevron_right, color: AppColors.accent, size: 16),
                ]),
              ),
            ),
        ],
      ),
    );
  }

  Widget _ligneMission(IconData icone, String texte, {bool gras = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Icon(icone, size: 16, color: AppColors.texteMuet),
        const SizedBox(width: 8),
        Expanded(
          child: Text(texte,
              style: TextStyle(fontSize: 13, fontWeight: gras ? FontWeight.bold : FontWeight.normal, color: AppColors.texte)),
        ),
      ]),
    );
  }

  String _dateAffichee(String iso) {
    final d = DateTime.tryParse(iso);
    if (d == null) return iso;
    final h = d.hour.toString().padLeft(2, '0');
    final m = d.minute.toString().padLeft(2, '0');
    return '${d.day}/${d.month}/${d.year} à $h:$m';
  }
}
