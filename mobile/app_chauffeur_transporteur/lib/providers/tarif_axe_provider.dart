import 'package:flutter_riverpod/legacy.dart';

// Categories de marchandise retenues (spec-tarification-par-chauffeur.md
// §1/§3) : standard/fragile/ultra fragile, jamais un continuum F/kg/km --
// un tarif a plat par axe et par categorie, fixe par le chauffeur lui-meme.
enum CategorieMarchandise { standard, fragile, ultraFragile }

class TarifAxe {
  final String axeId;
  final bool actif;
  final Map<CategorieMarchandise, double> tarifsParKg;

  const TarifAxe({required this.axeId, required this.actif, required this.tarifsParKg});

  TarifAxe copyWith({bool? actif, Map<CategorieMarchandise, double>? tarifsParKg}) {
    return TarifAxe(
      axeId: axeId,
      actif: actif ?? this.actif,
      tarifsParKg: tarifsParKg ?? this.tarifsParKg,
    );
  }

  static const _pasDeTarif = {
    CategorieMarchandise.standard: 70.0,
    CategorieMarchandise.fragile: 85.0,
    CategorieMarchandise.ultraFragile: 100.0,
  };

  factory TarifAxe.parDefaut(String axeId) => TarifAxe(axeId: axeId, actif: true, tarifsParKg: Map.of(_pasDeTarif));
}

class TarifAxeState {
  final Map<String, TarifAxe> tarifsParAxeId;
  const TarifAxeState({this.tarifsParAxeId = const {}});

  bool estConfigure(String axeId) => tarifsParAxeId.containsKey(axeId);

  TarifAxeState copyWith({Map<String, TarifAxe>? tarifsParAxeId}) {
    return TarifAxeState(tarifsParAxeId: tarifsParAxeId ?? this.tarifsParAxeId);
  }
}

// Pas encore de contrat backend pour le tarif par chauffeur/axe (le Moteur
// doit encore adapter TarificationL4Service/BaremeTarification cote
// service-opt pour un modele per-axe -- voir spec-tarification-par-chauffeur.md).
// Purement local pour l'instant, meme principe que axeSelectionneId dans
// axes_provider.dart : a brancher sur un vrai endpoint des que le contrat
// backend existe, sans changer la forme de ce provider cote UI.
class TarifAxeNotifier extends StateNotifier<TarifAxeState> {
  TarifAxeNotifier() : super(const TarifAxeState());

  void configurer(String axeId) {
    if (state.tarifsParAxeId.containsKey(axeId)) return;
    final maj = Map<String, TarifAxe>.of(state.tarifsParAxeId);
    maj[axeId] = TarifAxe.parDefaut(axeId);
    state = state.copyWith(tarifsParAxeId: maj);
  }

  void retirer(String axeId) {
    final maj = Map<String, TarifAxe>.of(state.tarifsParAxeId)..remove(axeId);
    state = state.copyWith(tarifsParAxeId: maj);
  }

  void toggleActif(String axeId) {
    final axe = state.tarifsParAxeId[axeId];
    if (axe == null) return;
    final maj = Map<String, TarifAxe>.of(state.tarifsParAxeId);
    maj[axeId] = axe.copyWith(actif: !axe.actif);
    state = state.copyWith(tarifsParAxeId: maj);
  }

  // Pas (5 F/kg) et plancher (5 F/kg, jamais 0 ni negatif) choisis pour
  // rester coherents avec les exemples de la spec (tarifs 70-115 F/kg).
  void ajusterTarif(String axeId, CategorieMarchandise categorie, int delta) {
    final axe = state.tarifsParAxeId[axeId];
    if (axe == null) return;
    final tarifs = Map<CategorieMarchandise, double>.of(axe.tarifsParKg);
    final valeur = (tarifs[categorie] ?? 0) + delta * 5;
    tarifs[categorie] = valeur < 5 ? 5 : valeur;
    final maj = Map<String, TarifAxe>.of(state.tarifsParAxeId);
    maj[axeId] = axe.copyWith(tarifsParKg: tarifs);
    state = state.copyWith(tarifsParAxeId: maj);
  }
}

final tarifAxeProvider = StateNotifierProvider<TarifAxeNotifier, TarifAxeState>((ref) {
  return TarifAxeNotifier();
});
