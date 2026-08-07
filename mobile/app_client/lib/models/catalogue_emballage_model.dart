class CatalogueEmballageModel {
  final String id;
  final String nom;
  final String icone;
  final double poidsUnitaireKg;
  final double volumeUnitaireM3;
  final bool fragileParDefaut;

  const CatalogueEmballageModel({
    required this.id,
    required this.nom,
    required this.icone,
    required this.poidsUnitaireKg,
    required this.volumeUnitaireM3,
    required this.fragileParDefaut,
  });

  factory CatalogueEmballageModel.fromJson(Map<String, dynamic> json) {
    return CatalogueEmballageModel(
      id: json['id'] ?? '',
      nom: json['nom'] ?? '',
      icone: json['icone'] ?? 'inventory_2',
      poidsUnitaireKg: (json['poidsUnitaireKg'] as num?)?.toDouble() ?? 0,
      volumeUnitaireM3: (json['volumeUnitaireM3'] as num?)?.toDouble() ?? 0,
      fragileParDefaut: json['fragileParDefaut'] ?? false,
    );
  }
}
