import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

class Profil {
  final String acteurId;
  final String type; // PARTICULIER ou ENTREPRISE
  final String? nom;
  final String? prenom;
  final String? raisonSociale;
  final String niveauKyc; // NIVEAU_0, NIVEAU_1, NIVEAU_2

  const Profil({
    required this.acteurId,
    required this.type,
    this.nom,
    this.prenom,
    this.raisonSociale,
    required this.niveauKyc,
  });

  factory Profil.fromJson(Map<String, dynamic> json) => Profil(
        acteurId: json['acteurId'] as String,
        type: json['type'] as String,
        nom: json['nom'] as String?,
        prenom: json['prenom'] as String?,
        raisonSociale: json['raisonSociale'] as String?,
        niveauKyc: json['niveauKyc'] as String,
      );
}

class KycState {
  final bool chargement;
  final String? erreur;
  final Profil? profil;

  const KycState({this.chargement = false, this.erreur, this.profil});

  KycState copyWith({bool? chargement, String? erreur, Profil? profil}) {
    return KycState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      profil: profil ?? this.profil,
    );
  }
}

// Contrat réel de la gateway (RG-011, Sprint 2) : GET/PUT /kyc/profil/**,
// niveau 1 uniquement (particulier ou entreprise) — voir ProfilController
// (backend/gateway/.../infrastructure/rest/ida/). Le niveau 2 (pièces
// justificatives) n'existe pas encore côté service-ida.
class KycNotifier extends StateNotifier<KycState> {
  final Dio _dio;

  KycNotifier(this._dio) : super(const KycState());

  Future<void> chargerProfil() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/kyc/profil');
      state = state.copyWith(chargement: false, profil: Profil.fromJson(response.data));
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
    }
  }

  Future<bool> completerParticulier(String nom, String prenom) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.put('/kyc/profil/particulier', data: {
        'nom': nom,
        'prenom': prenom,
      });
      state = state.copyWith(chargement: false, profil: Profil.fromJson(response.data));
      return true;
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
      return false;
    }
  }

  Future<bool> completerEntreprise(String raisonSociale, String? numeroRegistreCommerce) async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.put('/kyc/profil/entreprise', data: {
        'raisonSociale': raisonSociale,
        if (numeroRegistreCommerce != null && numeroRegistreCommerce.isNotEmpty)
          'numeroRegistreCommerce': numeroRegistreCommerce,
      });
      state = state.copyWith(chargement: false, profil: Profil.fromJson(response.data));
      return true;
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
      return false;
    }
  }

  String _messageErreur(DioException e) {
    final status = e.response?.statusCode;
    if (status == 400) {
      return (e.response?.data is Map ? e.response?.data['detail'] as String? : null) ??
          'Requête refusée.';
    }
    if (status == 503) return 'Service d\'identité momentanément indisponible.';
    return 'Erreur de connexion. Vérifiez votre réseau.';
  }
}

final kycProvider = StateNotifierProvider<KycNotifier, KycState>((ref) {
  return KycNotifier(ref.watch(dioProvider));
});
