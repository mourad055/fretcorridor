import 'dart:convert';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:geolocator/geolocator.dart';
import 'package:uuid/uuid.dart';
import 'dio_provider.dart';

const _keyFileEnrolements = 'agent_enrolements_file_offline';
const _uuid = Uuid();

// UC-IDA-03/RG-019/A1 : un enrôlement initié hors réseau est mis en file
// locale (chiffrée par flutter_secure_storage, backée par Keychain/Keystore)
// plutôt que perdu. idempotencyKey garantit qu'un rejeu au retour de réseau
// ne crée jamais deux enrôlements.
class DemandeEnrolement {
  final String idempotencyKey;
  final String telephone;
  final String typeActeur;
  final double latitude;
  final double longitude;

  const DemandeEnrolement({
    required this.idempotencyKey,
    required this.telephone,
    required this.typeActeur,
    required this.latitude,
    required this.longitude,
  });

  Map<String, dynamic> toJson() => {
        'idempotencyKey': idempotencyKey,
        'telephone': telephone,
        'typeActeur': typeActeur,
        'latitude': latitude,
        'longitude': longitude,
      };

  factory DemandeEnrolement.fromJson(Map<String, dynamic> json) => DemandeEnrolement(
        idempotencyKey: json['idempotencyKey'] as String,
        telephone: json['telephone'] as String,
        typeActeur: json['typeActeur'] as String,
        latitude: json['latitude'] as double,
        longitude: json['longitude'] as double,
      );
}

class EnrolementActif {
  final String enrolementId;
  final String telephone;
  final String typeActeur;
  final String statut; // EN_ATTENTE, ACTIVE

  const EnrolementActif({
    required this.enrolementId,
    required this.telephone,
    required this.typeActeur,
    required this.statut,
  });

  factory EnrolementActif.fromJson(Map<String, dynamic> json) => EnrolementActif(
        enrolementId: json['enrolementId'] as String,
        telephone: json['telephone'] as String,
        typeActeur: json['typeActeur'] as String,
        statut: json['statut'] as String,
      );
}

class AgentEnrolementState {
  final bool chargement;
  final String? erreur;
  final String? succes;
  final List<DemandeEnrolement> fileAttente;
  final EnrolementActif? dernierEnrolement;

  const AgentEnrolementState({
    this.chargement = false,
    this.erreur,
    this.succes,
    this.fileAttente = const [],
    this.dernierEnrolement,
  });

  AgentEnrolementState copyWith({
    bool? chargement,
    String? erreur,
    String? succes,
    List<DemandeEnrolement>? fileAttente,
    EnrolementActif? dernierEnrolement,
    bool effacerDernierEnrolement = false,
  }) {
    return AgentEnrolementState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      succes: succes,
      fileAttente: fileAttente ?? this.fileAttente,
      dernierEnrolement: effacerDernierEnrolement ? null : (dernierEnrolement ?? this.dernierEnrolement),
    );
  }
}

// Contrat réel de la gateway (RBAC AGENT) : POST /agent/enrolements,
// POST /agent/enrolements/{id}/activation — voir AgentEnrolementController
// (backend/gateway/.../infrastructure/rest/agent/).
class AgentEnrolementNotifier extends StateNotifier<AgentEnrolementState> {
  final Dio _dio;
  static const _storage = FlutterSecureStorage();

  AgentEnrolementNotifier(this._dio) : super(const AgentEnrolementState()) {
    _chargerFileDepuisDisque();
    Connectivity().onConnectivityChanged.listen((results) {
      if (!results.contains(ConnectivityResult.none)) {
        synchroniserFileAttente();
      }
    });
  }

  Future<void> _chargerFileDepuisDisque() async {
    final brut = await _storage.read(key: _keyFileEnrolements);
    if (brut == null) return;
    final liste = (jsonDecode(brut) as List<dynamic>)
        .map((e) => DemandeEnrolement.fromJson(e as Map<String, dynamic>))
        .toList();
    state = state.copyWith(fileAttente: liste);
    if (liste.isNotEmpty) synchroniserFileAttente();
  }

  Future<void> _sauvegarderFile(List<DemandeEnrolement> file) async {
    await _storage.write(key: _keyFileEnrolements, value: jsonEncode(file.map((d) => d.toJson()).toList()));
  }

  Future<Position?> _positionActuelle() async {
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied || permission == LocationPermission.deniedForever) {
      return null;
    }
    try {
      return await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(accuracy: LocationAccuracy.medium),
      );
    } catch (_) {
      return null;
    }
  }

  Future<void> initierEnrolement(String telephone, String typeActeur) async {
    state = state.copyWith(chargement: true, erreur: null, succes: null, effacerDernierEnrolement: true);

    final position = await _positionActuelle();
    if (position == null) {
      state = state.copyWith(chargement: false, erreur: 'Position GPS indisponible — activez la localisation.');
      return;
    }

    final demande = DemandeEnrolement(
      idempotencyKey: _uuid.v4(),
      telephone: telephone,
      typeActeur: typeActeur,
      latitude: position.latitude,
      longitude: position.longitude,
    );

    final enrolement = await _envoyer(demande);
    if (enrolement != null) {
      state = state.copyWith(
        chargement: false,
        dernierEnrolement: enrolement,
        succes: 'Code envoyé par SMS au ${demande.telephone}.',
      );
      return;
    }

    // Pas de réseau (ou service indisponible) : mis en file, jamais perdu.
    final nouvelleFile = [...state.fileAttente, demande];
    await _sauvegarderFile(nouvelleFile);
    state = state.copyWith(
      chargement: false,
      fileAttente: nouvelleFile,
      succes: 'Hors ligne — enrôlement mis en file, sera envoyé au retour du réseau.',
    );
  }

  /// Envoie une demande ; retourne null si l'échec est réseau (à mettre en
  /// file), relance une DioException pour tout refus métier (ex. téléphone
  /// déjà utilisé) que l'appelant doit afficher, pas mettre en file.
  Future<EnrolementActif?> _envoyer(DemandeEnrolement demande) async {
    try {
      final response = await _dio.post('/agent/enrolements', data: demande.toJson());
      return EnrolementActif.fromJson(response.data);
    } on DioException catch (e) {
      if (_estErreurReseau(e)) return null;
      rethrow;
    }
  }

  bool _estErreurReseau(DioException e) {
    return e.type == DioExceptionType.connectionError ||
        e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.response?.statusCode == 503;
  }

  Future<void> synchroniserFileAttente() async {
    if (state.fileAttente.isEmpty) return;
    final restantes = <DemandeEnrolement>[];
    for (final demande in state.fileAttente) {
      try {
        final envoyee = await _envoyer(demande);
        if (envoyee == null) restantes.add(demande); // toujours pas de réseau
      } on DioException {
        // Refus métier (ex. déjà activé entretemps) : on retire quand même
        // de la file, pas la peine de la rejouer indéfiniment.
      }
    }
    await _sauvegarderFile(restantes);
    state = state.copyWith(fileAttente: restantes);
  }

  Future<bool> activerEnrolement(String enrolementId, String otp, String codePin) async {
    state = state.copyWith(chargement: true, erreur: null, succes: null);
    try {
      final response = await _dio.post('/agent/enrolements/$enrolementId/activation', data: {
        'otp': otp,
        'codePin': codePin,
      });
      final enrolement = EnrolementActif.fromJson(response.data);
      state = state.copyWith(
        chargement: false,
        dernierEnrolement: enrolement,
        succes: 'Compte activé pour ${enrolement.telephone}.',
      );
      return true;
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
      return false;
    }
  }

  void reinitialiser() {
    state = state.copyWith(effacerDernierEnrolement: true, erreur: null, succes: null);
  }

  String _messageErreur(DioException e) {
    final status = e.response?.statusCode;
    if (status == 400) {
      return (e.response?.data is Map ? e.response?.data['detail'] as String? : null) ?? 'Requête refusée.';
    }
    if (status == 404) return 'Enrôlement introuvable.';
    if (status == 503) return 'Service d\'identité momentanément indisponible.';
    return 'Erreur de connexion. Vérifiez votre réseau.';
  }
}

final agentEnrolementProvider = StateNotifierProvider<AgentEnrolementNotifier, AgentEnrolementState>((ref) {
  return AgentEnrolementNotifier(ref.watch(dioProvider));
});
