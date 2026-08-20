class EtapeMissionModel {
  final String type;
  final String libelle;
  final DateTime? horodatageCapture;
  final DateTime horodatageTransmission;

  const EtapeMissionModel({
    required this.type,
    required this.libelle,
    this.horodatageCapture,
    required this.horodatageTransmission,
  });

  factory EtapeMissionModel.fromJson(Map<String, dynamic> json) {
    return EtapeMissionModel(
      type: json['type'] ?? '',
      libelle: json['libelle'] ?? '',
      horodatageCapture: json['horodatageCapture'] != null ? DateTime.parse(json['horodatageCapture']) : null,
      horodatageTransmission: DateTime.parse(json['horodatageTransmission']),
    );
  }
}

class ChronologieModel {
  final String missionId;
  final String statut;
  final List<EtapeMissionModel> etapes;
  final String? tourneeId;

  const ChronologieModel({required this.missionId, required this.statut, required this.etapes, this.tourneeId});

  factory ChronologieModel.fromJson(Map<String, dynamic> json) {
    return ChronologieModel(
      missionId: json['missionId'] ?? '',
      statut: json['statut'] ?? 'EN_ATTENTE',
      etapes: (json['etapes'] as List? ?? []).map((e) => EtapeMissionModel.fromJson(e)).toList(),
      tourneeId: json['tourneeId'] as String?,
    );
  }
}

class PositionModel {
  final double latitude;
  final double longitude;
  final DateTime horodatage;
  final int ageSecondes;

  const PositionModel({
    required this.latitude, required this.longitude, required this.horodatage, required this.ageSecondes,
  });

  factory PositionModel.fromJson(Map<String, dynamic> json) {
    return PositionModel(
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      horodatage: DateTime.parse(json['horodatage']),
      ageSecondes: json['ageSecondes'] ?? 0,
    );
  }
}
