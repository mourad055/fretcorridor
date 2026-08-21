import 'dart:async';
import 'dart:io';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'dio_provider.dart';

class Piece {
  final String typeDocument;
  final String? url;
  final String? dateDepot;

  const Piece({required this.typeDocument, this.url, this.dateDepot});

  factory Piece.fromJson(Map<String, dynamic> json) => Piece(
        typeDocument: json['typeDocument'] as String,
        url: json['url'] as String?,
        dateDepot: json['dateDepot'] as String?,
      );
}

class KycState {
  final bool chargement;
  final bool depotEnCours;
  final String? erreur;
  final String? succes;
  final String? type; // PARTICULIER ou ENTREPRISE
  final String? nom;
  final String? prenom;
  final String? raisonSociale;
  final String niveauKyc;
  final List<Piece> pieces;

  const KycState({
    this.chargement = false,
    this.depotEnCours = false,
    this.erreur,
    this.succes,
    this.type,
    this.nom,
    this.prenom,
    this.raisonSociale,
    this.niveauKyc = 'NIVEAU_0',
    this.pieces = const [],
  });

  // RG-011 : niveau 1 = identité déclarée ET au moins une pièce déposée —
  // pilote l'affichage des deux étapes de l'écran de complétion.
  bool get identiteDeclaree => (nom != null && prenom != null) || raisonSociale != null;
  bool get pieceDeposee => pieces.isNotEmpty;

  KycState copyWith({
    bool? chargement,
    bool? depotEnCours,
    String? erreur,
    String? succes,
    String? type,
    String? nom,
    String? prenom,
    String? raisonSociale,
    String? niveauKyc,
    List<Piece>? pieces,
  }) {
    return KycState(
      chargement: chargement ?? this.chargement,
      depotEnCours: depotEnCours ?? this.depotEnCours,
      erreur: erreur,
      succes: succes,
      type: type ?? this.type,
      nom: nom ?? this.nom,
      prenom: prenom ?? this.prenom,
      raisonSociale: raisonSociale ?? this.raisonSociale,
      niveauKyc: niveauKyc ?? this.niveauKyc,
      pieces: pieces ?? this.pieces,
    );
  }
}

class KycNotifier extends StateNotifier<KycState> {
  final Dio _dio;
  static const _storage = FlutterSecureStorage();
  Timer? _effacementErreur;

  KycNotifier(this._dio) : super(const KycState()) {
    chargerProfil();
  }

  // Ce provider est un singleton créé une seule fois pour toute la durée du
  // process app — sans cet appel explicite après login/inscription/logout,
  // il continue d'afficher le profil du COMPTE PRÉCÉDENT tant que l'app
  // n'est pas relancée (repéré : nouveau compte créé, écran affiche encore
  // l'ancien profil). À appeler après tout changement d'identité.
  void reinitialiser() {
    state = const KycState();
    chargerProfil();
  }

  @override
  void dispose() {
    _effacementErreur?.cancel();
    super.dispose();
  }

  void _afficherErreurTemporaire(String erreur) {
    _effacementErreur?.cancel();
    state = state.copyWith(chargement: false, depotEnCours: false, erreur: erreur);
    _effacementErreur = Timer(const Duration(seconds: 5), () {
      if (mounted) state = state.copyWith(erreur: null);
    });
  }

  Future<void> chargerProfil() async {
    try {
      final response = await _dio.get('/kyc/profil');
      state = state.copyWith(
        type: response.data['type'],
        nom: response.data['nom'],
        prenom: response.data['prenom'],
        raisonSociale: response.data['raisonSociale'],
        niveauKyc: response.data['niveauKyc'],
        pieces: _piecesDe(response.data),
      );
    } on DioException {
      // Pas encore de profil complété — normal juste après l'inscription
    }
  }

  Future<bool> completerParticulier({required String nom, required String prenom}) async {
    state = state.copyWith(chargement: true, erreur: null, succes: null);
    try {
      final response = await _dio.put('/kyc/profil/particulier', data: {
        'nom': nom,
        'prenom': prenom,
      });
      await _enregistrerNouveauxTokens(response.data);
      _appliquerProfil(response.data['profil'], succes: 'Identité enregistrée ✅');
      return true;
    } on DioException catch (e) {
      _afficherErreurTemporaire('Erreur : ${e.response?.data ?? e.message}');
      return false;
    }
  }

  Future<bool> completerEntreprise({required String raisonSociale, String? numeroRegistreCommerce}) async {
    state = state.copyWith(chargement: true, erreur: null, succes: null);
    try {
      final response = await _dio.put('/kyc/profil/entreprise', data: {
        'raisonSociale': raisonSociale,
        if (numeroRegistreCommerce != null) 'numeroRegistreCommerce': numeroRegistreCommerce,
      });
      await _enregistrerNouveauxTokens(response.data);
      _appliquerProfil(response.data['profil'], succes: 'Identité enregistrée ✅');
      return true;
    } on DioException catch (e) {
      _afficherErreurTemporaire('Erreur : ${e.response?.data ?? e.message}');
      return false;
    }
  }

  Future<bool> deposerDocument(String typeDocument, File fichier) async {
    state = state.copyWith(depotEnCours: true, erreur: null, succes: null);
    try {
      final formData = FormData.fromMap({
        'fichier': await MultipartFile.fromFile(fichier.path, filename: fichier.path.split('/').last),
        'typeDocument': typeDocument,
      });
      final response = await _dio.post('/kyc/documents', data: formData);
      await _enregistrerNouveauxTokens(response.data);
      state = state.copyWith(depotEnCours: false);
      _appliquerProfil(response.data['profil'], succes: 'Pièce déposée ✅');
      return true;
    } on DioException catch (e) {
      _afficherErreurTemporaire('Erreur : ${e.response?.data ?? e.message}');
      return false;
    }
  }

  void _appliquerProfil(Map<String, dynamic> profil, {required String succes}) {
    state = state.copyWith(
      chargement: false,
      depotEnCours: false,
      succes: succes,
      type: profil['type'],
      nom: profil['nom'],
      prenom: profil['prenom'],
      raisonSociale: profil['raisonSociale'],
      niveauKyc: profil['niveauKyc'],
      pieces: _piecesDe(profil),
    );
  }

  List<Piece> _piecesDe(Map<String, dynamic> profil) {
    return (profil['pieces'] as List<dynamic>? ?? [])
        .map((p) => Piece.fromJson(p as Map<String, dynamic>))
        .toList();
  }

  // Après complétion du profil, le niveauKyc change → nouveaux tokens
  // nécessaires pour que les futurs appels (ex: publier une demande) portent
  // le bon niveau à jour.
  Future<void> _enregistrerNouveauxTokens(Map<String, dynamic> data) async {
    await _storage.write(key: keyAccessToken, value: data['accessToken']);
    await _storage.write(key: keyRefreshToken, value: data['refreshToken']);
  }
}

final kycProvider = StateNotifierProvider<KycNotifier, KycState>((ref) {
  return KycNotifier(ref.watch(idaDioProvider));
});
