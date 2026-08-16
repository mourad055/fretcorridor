import 'package:flutter_riverpod/legacy.dart';

enum TypeEtapeTournee { enlevement, livraison }

enum StatutEtapeTournee { aVenir, enCours, terminee }

class EtapeTournee {
  final String id;
  final TypeEtapeTournee type;
  final String lieuNom;
  final String? envoiReference;
  final StatutEtapeTournee statut;

  const EtapeTournee({
    required this.id,
    required this.type,
    required this.lieuNom,
    this.envoiReference,
    required this.statut,
  });

  EtapeTournee copyWith({StatutEtapeTournee? statut}) => EtapeTournee(
        id: id,
        type: type,
        lieuNom: lieuNom,
        envoiReference: envoiReference,
        statut: statut ?? this.statut,
      );
}

class TourneeMultiEtapes {
  final String missionId;
  final String axeLibelle;
  final List<EtapeTournee> etapes;

  const TourneeMultiEtapes({required this.missionId, required this.axeLibelle, required this.etapes});

  int get indexEtapeCourante => etapes.indexWhere((e) => e.statut != StatutEtapeTournee.terminee);

  bool get terminee => indexEtapeCourante == -1;

  EtapeTournee? get etapeCourante => terminee ? null : etapes[indexEtapeCourante];

  List<EtapeTournee> get etapesTerminees => etapes.where((e) => e.statut == StatutEtapeTournee.terminee).toList();
}

class MissionMultiEtapesState {
  final TourneeMultiEtapes? tournee;

  const MissionMultiEtapesState({this.tournee});

  MissionMultiEtapesState copyWith({TourneeMultiEtapes? tournee}) => MissionMultiEtapesState(tournee: tournee ?? this.tournee);
}

/// S11 (Sprint 11, "Consolidation LTL moteur V1") — ⚠️ MOCK, aucun appel réseau.
///
/// service-opt (Moteur) n'expose pas encore le multi-étapes côté serveur ;
/// le topic Kafka `EtapeExecutee` est en cours de spec côté Moteur pour S12
/// (missionId = celui d'AffectationConfirmeeEvent, confirmé). Ce provider
/// simule localement une tournée de groupage (plusieurs enlèvements et/ou
/// livraisons consécutifs) pour permettre de construire et valider l'écran
/// dès maintenant. À remplacer par un vrai fetch (GET) + une confirmation
/// d'étape (POST) vers le gateway une fois service-opt/service-exe prêts —
/// même contrat que MissionExecutionController (S7), généralisé à N étapes.
class MissionMultiEtapesNotifier extends StateNotifier<MissionMultiEtapesState> {
  MissionMultiEtapesNotifier() : super(const MissionMultiEtapesState());

  void chargerTourneeDemo() {
    state = MissionMultiEtapesState(
      tournee: const TourneeMultiEtapes(
        missionId: 'mock-tournee-s11-001',
        axeLibelle: 'Douala → Yaoundé',
        etapes: [
          EtapeTournee(
            id: 'e1',
            type: TypeEtapeTournee.enlevement,
            lieuNom: 'Entrepôt Bonabéri (Client A)',
            envoiReference: 'ENV-1042',
            statut: StatutEtapeTournee.enCours,
          ),
          EtapeTournee(
            id: 'e2',
            type: TypeEtapeTournee.enlevement,
            lieuNom: 'Dépôt Akwa (Client B)',
            envoiReference: 'ENV-1043',
            statut: StatutEtapeTournee.aVenir,
          ),
          EtapeTournee(
            id: 'e3',
            type: TypeEtapeTournee.livraison,
            lieuNom: 'Mvan (destinataire Client A)',
            envoiReference: 'ENV-1042',
            statut: StatutEtapeTournee.aVenir,
          ),
          EtapeTournee(
            id: 'e4',
            type: TypeEtapeTournee.livraison,
            lieuNom: 'Nlongkak (destinataire Client B)',
            envoiReference: 'ENV-1043',
            statut: StatutEtapeTournee.aVenir,
          ),
        ],
      ),
    );
  }

  /// Confirme l'étape en cours et fait passer la suivante en "en cours" —
  /// entièrement local (MOCK), aucune écriture réseau.
  void confirmerEtapeCourante() {
    final tournee = state.tournee;
    if (tournee == null) return;
    final index = tournee.indexEtapeCourante;
    if (index == -1) return;

    final nouvellesEtapes = [...tournee.etapes];
    nouvellesEtapes[index] = nouvellesEtapes[index].copyWith(statut: StatutEtapeTournee.terminee);
    if (index + 1 < nouvellesEtapes.length) {
      nouvellesEtapes[index + 1] = nouvellesEtapes[index + 1].copyWith(statut: StatutEtapeTournee.enCours);
    }

    state = MissionMultiEtapesState(
      tournee: TourneeMultiEtapes(missionId: tournee.missionId, axeLibelle: tournee.axeLibelle, etapes: nouvellesEtapes),
    );
  }
}

final missionMultiEtapesProvider = StateNotifierProvider<MissionMultiEtapesNotifier, MissionMultiEtapesState>((ref) {
  return MissionMultiEtapesNotifier();
});
