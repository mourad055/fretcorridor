class NotificationModel {
  final String id;
  final String titre;
  final String corps;
  final String type;
  final String? referenceId;
  final bool lue;
  final DateTime dateCreation;

  const NotificationModel({
    required this.id, required this.titre, required this.corps, required this.type,
    this.referenceId, required this.lue, required this.dateCreation,
  });

  factory NotificationModel.fromJson(Map<String, dynamic> json) {
    return NotificationModel(
      id: json['id'] ?? '',
      titre: json['titre'] ?? '',
      corps: json['corps'] ?? '',
      type: json['type'] ?? 'INFO_GENERALE',
      referenceId: json['referenceId'],
      lue: json['lue'] ?? false,
      dateCreation: DateTime.parse(json['dateCreation']),
    );
  }
}
