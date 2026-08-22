import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import '../models/chronologie_model.dart';
import 'dio_provider.dart';

class SuiviState {
  final bool chargement;
  final ChronologieModel? chronologie;
  final PositionModel? position;
  final String? lieuActuel;
  final String? erreur;

  const SuiviState({this.chargement = false, this.chronologie, this.position, this.lieuActuel, this.erreur});

  SuiviState copyWith({bool? chargement, ChronologieModel? chronologie, PositionModel? position, String? lieuActuel, String? erreur}) {
    return SuiviState(
      chargement: chargement ?? this.chargement,
      chronologie: chronologie ?? this.chronologie,
      position: position ?? this.position,
      lieuActuel: lieuActuel ?? this.lieuActuel,
      erreur: erreur,
    );
  }
}

class SuiviNotifier extends StateNotifier<SuiviState> {
  // La chronologie (service-exe) et la dernière position (service-flt) sont
  // deux microservices distincts, chacun avec son propre client.
  final Dio _dioExe;
  final Dio _dioFlt;
  SuiviNotifier(this._dioExe, this._dioFlt) : super(const SuiviState());

  // S7 puis S6 : la chronologie donne le missionId, nécessaire pour la position.
  // Tant qu'aucune mission n'existe encore pour cette demande (matching pas
  // encore actif — S5 est un stub), les deux resteront simplement vides.
  //
  // BUG CORRIGE : copyWith(chronologie: null, ...) ne vide jamais le champ —
  // `null ?? this.chronologie` retombe sur l'ancienne valeur (copyWith ne
  // distingue pas "non fourni" de "explicitement null"). Résultat : ouvrir
  // le suivi d'une demande sans mission encore affichait le suivi de la
  // DERNIÈRE demande consultée qui, elle, en avait un. État repartI à zéro
  // ici (nouvel objet, pas copyWith) au tout début de chaque chargement.
  Future<void> charger(String demandeId) async {
    state = const SuiviState(chargement: true);
    try {
      final response = await _dioExe.get('/missions/demande/$demandeId/chronologie');
      if (response.statusCode == 204 || response.data == null) {
        state = const SuiviState(chargement: false);
        return;
      }
      final chronologie = ChronologieModel.fromJson(response.data);
      state = state.copyWith(chargement: false, chronologie: chronologie);
      await _chargerPosition(chronologie.missionId);
    } on DioException catch (e) {
      if (e.response?.statusCode == 204) {
        state = const SuiviState(chargement: false);
      } else {
        state = SuiviState(chargement: false, erreur: 'Impossible de charger le suivi.');
      }
    }
  }

  Future<void> _chargerPosition(String missionId) async {
    try {
      final response = await _dioFlt.get('/positions/mission/$missionId/derniere');
      if (response.statusCode == 200 && response.data != null) {
        final position = PositionModel.fromJson(response.data);
        state = state.copyWith(position: position);
        unawaited(_resoudreLieu(position.latitude, position.longitude));
      }
    } on DioException {
      // Pas de position disponible — état normal tant que le chauffeur n'en envoie pas
    }
  }

  // "Véhicule en mouvement" sans plus de détail ne veut rien dire pour le
  // chargeur (retour utilisateur direct) — traduction des coordonnées brutes
  // en lieu lisible (ville/quartier) via Nominatim (OpenStreetMap, gratuit,
  // sans clé). Best-effort pur : un échec (réseau, quota) laisse simplement
  // lieuActuel à null, l'écran retombe sur le texte générique existant.
  Future<void> _resoudreLieu(double latitude, double longitude) async {
    try {
      final reponse = await Dio().get(
        'https://nominatim.openstreetmap.org/reverse',
        queryParameters: {'format': 'jsonv2', 'lat': latitude, 'lon': longitude, 'zoom': 14},
        options: Options(headers: {'User-Agent': 'FretCorridor/1.0 (suivi livraison)'}),
      );
      final adresse = reponse.data?['address'] as Map<String, dynamic>?;
      if (adresse == null) return;
      final lieu = adresse['suburb'] ?? adresse['quarter'] ?? adresse['neighbourhood'] ??
          adresse['town'] ?? adresse['city'] ?? adresse['village'] ?? adresse['county'];
      final ville = adresse['city'] ?? adresse['town'] ?? adresse['county'];
      final texte = (lieu != null && ville != null && lieu != ville) ? '$lieu, $ville' : (lieu ?? ville);
      if (texte != null && mounted) state = state.copyWith(lieuActuel: texte as String);
    } catch (_) {
      // Best-effort — pas de lieu résolu, l'écran garde le texte générique.
    }
  }
}

final suiviProvider = StateNotifierProvider<SuiviNotifier, SuiviState>((ref) {
  return SuiviNotifier(ref.watch(exeDioProvider), ref.watch(fltDioProvider));
});
