import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

// Categories de marchandise retenues (spec-tarification-par-chauffeur.md
// §1/§3) : standard/fragile/ultra fragile, jamais un continuum F/kg/km --
// un tarif a plat par axe et par categorie, fixe par le chauffeur lui-meme.
enum CategorieMarchandise { standard, fragile, ultraFragile }

String _versBackend(CategorieMarchandise c) => switch (c) {
      CategorieMarchandise.standard => 'STANDARD',
      CategorieMarchandise.fragile => 'FRAGILE',
      CategorieMarchandise.ultraFragile => 'ULTRA_FRAGILE',
    };

CategorieMarchandise _depuisBackend(String c) => switch (c) {
      'FRAGILE' => CategorieMarchandise.fragile,
      'ULTRA_FRAGILE' => CategorieMarchandise.ultraFragile,
      _ => CategorieMarchandise.standard,
    };

// Une ligne = un tarif reellement enregistre cote service-cap (PR #145,
// GET /api/v1/mes-tarifs via le gateway) pour un axe+categorie donnes.
class LigneTarif {
  final String id;
  final CategorieMarchandise categorie;
  final double tarifFcfaParKg;
  final bool actif;

  const LigneTarif({required this.id, required this.categorie, required this.tarifFcfaParKg, required this.actif});

  factory LigneTarif.fromJson(Map<String, dynamic> json) => LigneTarif(
        id: json['id'] as String,
        categorie: _depuisBackend(json['categorieMarchandise'] as String),
        tarifFcfaParKg: (json['tarifFcfaParKg'] as num).toDouble(),
        actif: json['actif'] as bool,
      );

  LigneTarif copyWith({double? tarifFcfaParKg, bool? actif}) => LigneTarif(
        id: id,
        categorie: categorie,
        tarifFcfaParKg: tarifFcfaParKg ?? this.tarifFcfaParKg,
        actif: actif ?? this.actif,
      );
}

// Vue groupee par axe, pour l'ecran (une carte par axe, une ligne par
// categorie configuree). "actif" agrege : vrai si au moins une ligne est
// active -- le bouton pause/activer de l'axe agit alors sur les 3 a la fois.
class TarifAxe {
  final String axeId;
  final List<LigneTarif> lignes;

  const TarifAxe({required this.axeId, required this.lignes});

  bool get actif => lignes.any((l) => l.actif);

  Map<CategorieMarchandise, double> get tarifsParKg =>
      {for (final l in lignes) l.categorie: l.tarifFcfaParKg};

  LigneTarif? ligne(CategorieMarchandise c) {
    for (final l in lignes) {
      if (l.categorie == c) return l;
    }
    return null;
  }
}

class TarifAxeState {
  final bool chargement;
  final String? erreur;
  final Map<String, TarifAxe> tarifsParAxeId;

  const TarifAxeState({this.chargement = false, this.erreur, this.tarifsParAxeId = const {}});

  bool estConfigure(String axeId) => tarifsParAxeId.containsKey(axeId);

  TarifAxeState copyWith({bool? chargement, String? erreur, Map<String, TarifAxe>? tarifsParAxeId}) {
    return TarifAxeState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      tarifsParAxeId: tarifsParAxeId ?? this.tarifsParAxeId,
    );
  }
}

// Contrat backend livre (PR #145, service-cap + gateway) : CRUD reel sur
// GET/POST/PATCH/DELETE /api/v1/mes-tarifs(/{id}(/actif)). "Configurer" cree
// les 3 lignes (standard/fragile/ultra fragile) d'un coup, aux valeurs par
// defaut de la spec -- le chauffeur les ajuste ensuite une a une.
class TarifAxeNotifier extends StateNotifier<TarifAxeState> {
  final Dio _dio;

  TarifAxeNotifier(this._dio) : super(const TarifAxeState());

  static const Map<CategorieMarchandise, double> _valeursParDefaut = {
    CategorieMarchandise.standard: 70.0,
    CategorieMarchandise.fragile: 85.0,
    CategorieMarchandise.ultraFragile: 100.0,
  };

  Future<void> charger() async {
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/mes-tarifs');
      final lignes = (response.data as List<dynamic>)
          .map((j) => _AxeEtLigne.fromJson(j as Map<String, dynamic>))
          .toList();
      final parAxeId = <String, List<LigneTarif>>{};
      for (final el in lignes) {
        parAxeId.putIfAbsent(el.axeId, () => []).add(el.ligne);
      }
      state = state.copyWith(
        chargement: false,
        tarifsParAxeId: parAxeId.map((axeId, l) => MapEntry(axeId, TarifAxe(axeId: axeId, lignes: l))),
      );
    } on DioException {
      state = state.copyWith(chargement: false, erreur: 'Erreur de connexion. Vérifiez votre réseau.');
    }
  }

  Future<void> configurer(String axeId) async {
    if (state.tarifsParAxeId.containsKey(axeId)) return;
    try {
      final lignes = <LigneTarif>[];
      for (final entry in _valeursParDefaut.entries) {
        final response = await _dio.post('/mes-tarifs', data: {
          'axeId': axeId,
          'categorieMarchandise': _versBackend(entry.key),
          'tarifFcfaParKg': entry.value,
        });
        lignes.add(LigneTarif.fromJson(response.data as Map<String, dynamic>));
      }
      final maj = Map<String, TarifAxe>.of(state.tarifsParAxeId);
      maj[axeId] = TarifAxe(axeId: axeId, lignes: lignes);
      state = state.copyWith(tarifsParAxeId: maj);
    } on DioException {
      state = state.copyWith(erreur: 'Erreur de connexion. Vérifiez votre réseau.');
    }
  }

  Future<void> retirer(String axeId) async {
    final axe = state.tarifsParAxeId[axeId];
    if (axe == null) return;
    try {
      for (final ligne in axe.lignes) {
        await _dio.delete('/mes-tarifs/${ligne.id}');
      }
      final maj = Map<String, TarifAxe>.of(state.tarifsParAxeId)..remove(axeId);
      state = state.copyWith(tarifsParAxeId: maj);
    } on DioException {
      state = state.copyWith(erreur: 'Erreur de connexion. Vérifiez votre réseau.');
    }
  }

  Future<void> toggleActif(String axeId) async {
    final axe = state.tarifsParAxeId[axeId];
    if (axe == null) return;
    final cible = !axe.actif;
    try {
      final lignes = <LigneTarif>[];
      for (final ligne in axe.lignes) {
        if (ligne.actif == cible) {
          lignes.add(ligne);
          continue;
        }
        final response = await _dio.patch('/mes-tarifs/${ligne.id}/actif');
        lignes.add(LigneTarif.fromJson(response.data as Map<String, dynamic>));
      }
      final maj = Map<String, TarifAxe>.of(state.tarifsParAxeId);
      maj[axeId] = TarifAxe(axeId: axeId, lignes: lignes);
      state = state.copyWith(tarifsParAxeId: maj);
    } on DioException {
      state = state.copyWith(erreur: 'Erreur de connexion. Vérifiez votre réseau.');
    }
  }

  // Pas (5 F/kg) et plancher (5 F/kg, jamais 0 ni negatif) choisis pour
  // rester coherents avec les exemples de la spec (tarifs 70-115 F/kg).
  // Mise a jour optimiste (affichage immediat), annulee si l'appel echoue.
  Future<void> ajusterTarif(String axeId, CategorieMarchandise categorie, int delta) async {
    final axe = state.tarifsParAxeId[axeId];
    final ligne = axe?.ligne(categorie);
    if (axe == null || ligne == null) return;
    final valeur = ligne.tarifFcfaParKg + delta * 5;
    final nouvelleValeur = valeur < 5 ? 5.0 : valeur;

    final maj = Map<String, TarifAxe>.of(state.tarifsParAxeId);
    maj[axeId] = TarifAxe(
      axeId: axeId,
      lignes: [for (final l in axe.lignes) l.categorie == categorie ? l.copyWith(tarifFcfaParKg: nouvelleValeur) : l],
    );
    state = state.copyWith(tarifsParAxeId: maj);

    try {
      await _dio.patch('/mes-tarifs/${ligne.id}', data: {'tarifFcfaParKg': nouvelleValeur});
    } on DioException {
      final revert = Map<String, TarifAxe>.of(state.tarifsParAxeId);
      revert[axeId] = axe;
      state = state.copyWith(erreur: 'Erreur de connexion. Vérifiez votre réseau.', tarifsParAxeId: revert);
    }
  }
}

class _AxeEtLigne {
  final String axeId;
  final LigneTarif ligne;
  const _AxeEtLigne(this.axeId, this.ligne);

  factory _AxeEtLigne.fromJson(Map<String, dynamic> json) =>
      _AxeEtLigne(json['axeId'] as String, LigneTarif.fromJson(json));
}

final tarifAxeProvider = StateNotifierProvider<TarifAxeNotifier, TarifAxeState>((ref) {
  return TarifAxeNotifier(ref.watch(dioProvider));
});
