import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

// Trajets preenregistres (plan §5) : raccourci de saisie, pre-remplit le
// formulaire de publication -- ne publie jamais de demande tout seul.
class TrajetPreenregistre {
  final String id;
  final String nom;
  final String villeDepart;
  final String villeArrivee;
  final String typeEmballageId;
  final String typeEmballageNom;
  final int quantite;
  final bool fragile;
  final bool perissable;
  final bool dangereuse;
  final bool grandeValeur;
  final String categorieMarchandise;
  final String modeCollecte;
  final String destinataireNom;
  final String destinataireTelephone;

  const TrajetPreenregistre({
    required this.id,
    required this.nom,
    required this.villeDepart,
    required this.villeArrivee,
    required this.typeEmballageId,
    required this.typeEmballageNom,
    required this.quantite,
    required this.fragile,
    required this.perissable,
    required this.dangereuse,
    required this.grandeValeur,
    required this.categorieMarchandise,
    required this.modeCollecte,
    required this.destinataireNom,
    required this.destinataireTelephone,
  });

  factory TrajetPreenregistre.fromJson(Map<String, dynamic> json) => TrajetPreenregistre(
        id: json['id'] as String,
        nom: json['nom'] as String,
        villeDepart: json['villeDepart'] as String,
        villeArrivee: json['villeArrivee'] as String,
        typeEmballageId: json['typeEmballageId'] as String,
        typeEmballageNom: json['typeEmballageNom'] as String,
        quantite: json['quantite'] as int,
        fragile: json['fragile'] as bool? ?? false,
        perissable: json['perissable'] as bool? ?? false,
        dangereuse: json['dangereuse'] as bool? ?? false,
        grandeValeur: json['grandeValeur'] as bool? ?? false,
        categorieMarchandise: json['categorieMarchandise'] as String? ?? 'STANDARD',
        modeCollecte: json['modeCollecte'] as String,
        destinataireNom: json['destinataireNom'] as String,
        destinataireTelephone: json['destinataireTelephone'] as String,
      );
}

class TrajetPreenregistreState {
  final bool chargement;
  final String? erreur;
  final List<TrajetPreenregistre> trajets;

  const TrajetPreenregistreState({this.chargement = false, this.erreur, this.trajets = const []});

  TrajetPreenregistreState copyWith({bool? chargement, String? erreur, List<TrajetPreenregistre>? trajets}) {
    return TrajetPreenregistreState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      trajets: trajets ?? this.trajets,
    );
  }
}

class TrajetPreenregistreNotifier extends StateNotifier<TrajetPreenregistreState> {
  final Dio _dio;

  TrajetPreenregistreNotifier(this._dio) : super(const TrajetPreenregistreState()) {
    charger();
  }

  Future<void> charger() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/trajets-preenregistres/mes');
      state = state.copyWith(
        chargement: false,
        trajets: (response.data as List).map((t) => TrajetPreenregistre.fromJson(t)).toList(),
      );
    } on DioException {
      state = state.copyWith(chargement: false, erreur: 'Impossible de charger vos trajets enregistrés.');
    }
  }

  Future<bool> enregistrer({
    required String nom,
    required String villeDepart,
    required String villeArrivee,
    required String typeEmballageId,
    required int quantite,
    required bool fragile,
    required bool perissable,
    required bool dangereuse,
    required bool grandeValeur,
    required String categorieMarchandise,
    required String modeCollecte,
    required String destinataireNom,
    required String destinataireTelephone,
  }) async {
    try {
      await _dio.post('/trajets-preenregistres', data: {
        'nom': nom,
        'villeDepart': villeDepart,
        'villeArrivee': villeArrivee,
        'typeEmballageId': typeEmballageId,
        'quantite': quantite,
        'fragile': fragile,
        'perissable': perissable,
        'dangereuse': dangereuse,
        'grandeValeur': grandeValeur,
        'categorieMarchandise': categorieMarchandise,
        'modeCollecte': modeCollecte,
        'destinataireNom': destinataireNom,
        'destinataireTelephone': destinataireTelephone,
      });
      await charger();
      return true;
    } on DioException {
      return false;
    }
  }

  Future<bool> supprimer(String id) async {
    try {
      await _dio.delete('/trajets-preenregistres/$id');
      await charger();
      return true;
    } on DioException {
      return false;
    }
  }
}

final trajetPreenregistreProvider = StateNotifierProvider<TrajetPreenregistreNotifier, TrajetPreenregistreState>((ref) {
  return TrajetPreenregistreNotifier(ref.watch(mktDioProvider));
});
