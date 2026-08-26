import 'dart:async';
import 'dart:convert';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:geolocator/geolocator.dart';
import 'package:uuid/uuid.dart';
import 'dio_provider.dart';

const _intervalleEnvoi = Duration(seconds: 30);
const _keyFilePositionsOffline = 'positions_file_offline';

// Bloquant audit CDC du 19 août : aucune file locale pour le suivi GPS,
// contrairement au patron déjà en place pour l'enrôlement agent
// (idempotencyKey + retry + flutter_secure_storage, cf.
// agent_enrolement_provider.dart) — une coupure réseau prolongée perdait
// silencieusement toutes les positions capturées pendant la coupure.
class PositionAEnvoyer {
  final String missionId;
  final double latitude;
  final double longitude;
  final String horodatage;

  // FIX audit 21/08 (position-brute.yaml:38-45, ENF-SEC-03) : clé
  // d'idempotence générée À LA CAPTURE et conservée dans la file offline —
  // un même ré-envoi après coupure réseau porte toujours le même eventId et
  // est dédupliqué côté service-trk (UNIQUE(event_id)) au lieu de créer un
  // doublon. sourceCapture respecte l'enum du contrat (la valeur historique
  // "MOBILE_CHAUFFEUR" était rejetée par la CHECK de service-trk).
  final String eventId;
  final String sourceCapture;
  final double? precisionMetres;

  const PositionAEnvoyer({
    required this.missionId,
    required this.latitude,
    required this.longitude,
    required this.horodatage,
    required this.eventId,
    this.sourceCapture = 'GPS_NATIF',
    this.precisionMetres,
  });

  Map<String, dynamic> toJson() => {
        'missionId': missionId,
        'latitude': latitude,
        'longitude': longitude,
        'horodatage': horodatage,
        'eventId': eventId,
        'sourceCapture': sourceCapture,
        if (precisionMetres != null) 'precisionMetres': precisionMetres,
      };

  factory PositionAEnvoyer.fromJson(Map<String, dynamic> json) => PositionAEnvoyer(
        missionId: json['missionId'] as String,
        latitude: json['latitude'] as double,
        longitude: json['longitude'] as double,
        horodatage: json['horodatage'] as String,
        eventId: json['eventId'] as String? ?? const Uuid().v4(),
        sourceCapture: json['sourceCapture'] as String? ?? 'GPS_NATIF',
        precisionMetres: json['precisionMetres'] as double?,
      );
}

class PositionState {
  final bool suiviActif;
  final String? missionId;
  final DateTime? dernierEnvoi;
  final String? erreur;
  final int fileAttenteTaille;

  const PositionState({
    this.suiviActif = false,
    this.missionId,
    this.dernierEnvoi,
    this.erreur,
    this.fileAttenteTaille = 0,
  });

  PositionState copyWith({
    bool? suiviActif,
    String? missionId,
    DateTime? dernierEnvoi,
    String? erreur,
    int? fileAttenteTaille,
  }) {
    return PositionState(
      suiviActif: suiviActif ?? this.suiviActif,
      missionId: missionId ?? this.missionId,
      dernierEnvoi: dernierEnvoi ?? this.dernierEnvoi,
      erreur: erreur,
      fileAttenteTaille: fileAttenteTaille ?? this.fileAttenteTaille,
    );
  }
}

// S6 (EF-TRK-01) : capture + envoi périodique en arrière-plan pendant qu'une
// mission est active — voir EnvoiPositionController (gateway) et
// PositionController (service-flt). démarrerSuivi(missionId) est conçu pour
// être appelé automatiquement par l'écran de mission en cours (S7) une fois
// disponible ; en attendant, l'écran expose une saisie manuelle du missionId.
class PositionNotifier extends StateNotifier<PositionState> {
  final Dio _dio;
  static const _storage = FlutterSecureStorage();
  Timer? _timer;
  List<PositionAEnvoyer> _fileAttente = [];

  PositionNotifier(this._dio) : super(const PositionState()) {
    _chargerFileDepuisDisque();
    Connectivity().onConnectivityChanged.listen((results) {
      if (!results.contains(ConnectivityResult.none)) {
        _synchroniserFileAttente();
      }
    });
  }

  Future<void> _chargerFileDepuisDisque() async {
    final brut = await _storage.read(key: _keyFilePositionsOffline);
    if (brut == null) return;
    _fileAttente = (jsonDecode(brut) as List<dynamic>)
        .map((e) => PositionAEnvoyer.fromJson(e as Map<String, dynamic>))
        .toList();
    state = state.copyWith(fileAttenteTaille: _fileAttente.length);
    if (_fileAttente.isNotEmpty) _synchroniserFileAttente();
  }

  Future<void> _sauvegarderFile() async {
    await _storage.write(
      key: _keyFilePositionsOffline,
      value: jsonEncode(_fileAttente.map((p) => p.toJson()).toList()),
    );
  }

  void demarrerSuivi(String missionId) {
    _timer?.cancel();
    state = state.copyWith(suiviActif: true, missionId: missionId, erreur: null);
    _envoyerUnePosition();
    _timer = Timer.periodic(_intervalleEnvoi, (_) => _envoyerUnePosition());
  }

  void arreterSuivi() {
    _timer?.cancel();
    _timer = null;
    state = state.copyWith(suiviActif: false);
  }

  Future<void> _envoyerUnePosition() async {
    final missionId = state.missionId;
    if (missionId == null) return;

    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied || permission == LocationPermission.deniedForever) {
      state = state.copyWith(erreur: 'Position GPS indisponible — activez la localisation.');
      return;
    }

    try {
      // Priorite au cache (quasi instantane) plutot qu'un fix haute precision
      // frais : constate en test qu'un fix "high accuracy" peut rester
      // bloque en interieur bien au-dela de tout delai raisonnable (le
      // Future de Geolocator ne se resout jamais, meme avec timeLimit),
      // laissant chaque tick de _intervalleEnvoi mort - le suivi cote Client
      // ne reçoit alors jamais rien. Une position en cache, meme un peu
      // datee, alimente le suivi immediatement ; le prochain tick (30s)
      // retente et affine si un fix frais finit par arriver entre-temps.
      Position? position = await Geolocator.getLastKnownPosition();
      if (position == null) {
        try {
          position = await Geolocator.getCurrentPosition(
            locationSettings: const LocationSettings(
              accuracy: LocationAccuracy.medium,
              timeLimit: Duration(seconds: 10),
            ),
          );
        } catch (_) {
          position = null;
        }
      }
      if (position == null) {
        state = state.copyWith(erreur: 'Position GPS indisponible.');
        return;
      }
      final capture = PositionAEnvoyer(
        missionId: missionId,
        latitude: position.latitude,
        longitude: position.longitude,
        horodatage: DateTime.now().toIso8601String(),
        eventId: const Uuid().v4(),
        precisionMetres: position.accuracy,
      );

      final envoyee = await _envoyer(capture);
      if (envoyee) {
        state = state.copyWith(dernierEnvoi: DateTime.now(), erreur: null);
        return;
      }

      // Pas de réseau (ou service indisponible) : mise en file, jamais perdue.
      _fileAttente = [..._fileAttente, capture];
      await _sauvegarderFile();
      state = state.copyWith(
        erreur: 'Hors ligne — position mise en file, sera envoyée au retour du réseau.',
        fileAttenteTaille: _fileAttente.length,
      );
    } on DioException catch (e) {
      state = state.copyWith(erreur: _messageErreur(e));
    } catch (_) {
      state = state.copyWith(erreur: 'Position GPS indisponible.');
    }
  }

  /// Envoie une position ; retourne false si l'échec est réseau (à mettre en
  /// file), relance une DioException pour tout refus métier (ex. mission
  /// inconnue) que l'appelant doit afficher, pas mettre en file.
  Future<bool> _envoyer(PositionAEnvoyer capture) async {
    try {
      await _dio.post('/positions', data: capture.toJson());
      return true;
    } on DioException catch (e) {
      if (_estErreurReseau(e)) return false;
      rethrow;
    }
  }

  bool _estErreurReseau(DioException e) {
    return e.type == DioExceptionType.connectionError ||
        e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.response?.statusCode == 503;
  }

  Future<void> _synchroniserFileAttente() async {
    if (_fileAttente.isEmpty) return;
    final restantes = <PositionAEnvoyer>[];
    for (final capture in _fileAttente) {
      try {
        final envoyee = await _envoyer(capture);
        if (!envoyee) restantes.add(capture); // toujours pas de réseau
      } on DioException {
        // Refus métier (ex. mission close entretemps) : on retire quand même
        // de la file, pas la peine de la rejouer indéfiniment.
      }
    }
    _fileAttente = restantes;
    await _sauvegarderFile();
    state = state.copyWith(fileAttenteTaille: _fileAttente.length);
  }

  String _messageErreur(DioException e) {
    final status = e.response?.statusCode;
    if (status == 400) {
      return (e.response?.data is Map ? e.response?.data['detail'] as String? : null) ?? 'Position refusée.';
    }
    return 'Erreur réseau — nouvelle tentative au prochain envoi.';
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}

final positionProvider = StateNotifierProvider<PositionNotifier, PositionState>((ref) {
  return PositionNotifier(ref.watch(dioProvider));
});
