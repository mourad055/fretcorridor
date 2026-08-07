class DemandeModel {
  final String id;
  final String villeDepart;
  final String villeArrivee;
  final String typeEmballageNom;
  final int quantite;
  final double poidsTotalKg;
  final double volumeTotalM3;
  final double poidsTaxableKg;
  final bool fragile;
  final bool perissable;
  final bool dangereuse;
  final bool grandeValeur;
  final String typeDisponibilite;
  final String modeCollecte;
  final String destinataireNom;
  final String destinataireTelephone;
  final String statut;
  final DateTime dateCreation;

  const DemandeModel({
    required this.id,
    required this.villeDepart,
    required this.villeArrivee,
    required this.typeEmballageNom,
    required this.quantite,
    required this.poidsTotalKg,
    required this.volumeTotalM3,
    required this.poidsTaxableKg,
    required this.fragile,
    required this.perissable,
    required this.dangereuse,
    required this.grandeValeur,
    required this.typeDisponibilite,
    required this.modeCollecte,
    required this.destinataireNom,
    required this.destinataireTelephone,
    required this.statut,
    required this.dateCreation,
  });

  factory DemandeModel.fromJson(Map<String, dynamic> json) {
    return DemandeModel(
      id: json['id'] ?? '',
      villeDepart: json['villeDepart'] ?? '',
      villeArrivee: json['villeArrivee'] ?? '',
      typeEmballageNom: json['typeEmballageNom'] ?? '',
      quantite: json['quantite'] ?? 0,
      poidsTotalKg: (json['poidsTotalKg'] as num?)?.toDouble() ?? 0,
      volumeTotalM3: (json['volumeTotalM3'] as num?)?.toDouble() ?? 0,
      poidsTaxableKg: (json['poidsTaxableKg'] as num?)?.toDouble() ?? 0,
      fragile: json['fragile'] ?? false,
      perissable: json['perissable'] ?? false,
      dangereuse: json['dangereuse'] ?? false,
      grandeValeur: json['grandeValeur'] ?? false,
      typeDisponibilite: json['typeDisponibilite'] ?? 'DES_QUE_POSSIBLE',
      modeCollecte: json['modeCollecte'] ?? 'DOMICILE',
      destinataireNom: json['destinataireNom'] ?? '',
      destinataireTelephone: json['destinataireTelephone'] ?? '',
      statut: json['statut'] ?? 'PUBLIEE',
      dateCreation: json['dateCreation'] != null
          ? DateTime.parse(json['dateCreation'])
          : DateTime.now(),
    );
  }
}
