import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'dio_provider.dart';

class AppNotification {
  final String id;
  final String titre;
  final String corps;
  final String type;
  final String? referenceId;
  final bool lue;
  final String dateCreation;

  const AppNotification({
    required this.id,
    required this.titre,
    required this.corps,
    required this.type,
    this.referenceId,
    required this.lue,
    required this.dateCreation,
  });

  factory AppNotification.fromJson(Map<String, dynamic> json) => AppNotification(
        id: json['id'] as String,
        titre: json['titre'] as String,
        corps: json['corps'] as String,
        type: json['type'] as String,
        referenceId: json['referenceId'] as String?,
        lue: json['lue'] as bool,
        dateCreation: json['dateCreation'] as String,
      );
}

class NotificationState {
  final bool chargement;
  final String? erreur;
  final List<AppNotification> notifications;

  const NotificationState({this.chargement = false, this.erreur, this.notifications = const []});

  int get nombreNonLues => notifications.where((n) => !n.lue).length;

  NotificationState copyWith({bool? chargement, String? erreur, List<AppNotification>? notifications}) {
    return NotificationState(
      chargement: chargement ?? this.chargement,
      erreur: erreur,
      notifications: notifications ?? this.notifications,
    );
  }
}

// S9 (réception "tirée") : GET /notifications/mes, PATCH .../lue — voir
// NotificationMobileController (backend/gateway/.../infrastructure/rest/not/).
// Le push FCM effectif (réception hors application) n'est pas câblé : aucun
// projet Firebase disponible pour ce dépôt — écart documenté, pas un oubli.
// Aucun push FCM (pas de projet Firebase disponible) — le seul moyen de
// savoir qu'une notification est arrivée pendant que l'app est ouverte est
// de sonder périodiquement. Retour utilisateur direct (22 août) : une
// notification créée côté serveur restait invisible tant que l'écran
// Notifications n'était pas rouvert manuellement — sondage + son/vibration
// à la première hausse du nombre de non-lues.
class NotificationNotifier extends StateNotifier<NotificationState> {
  final Dio _dio;
  Timer? _sondage;

  NotificationNotifier(this._dio) : super(const NotificationState()) {
    charger();
    _sondage = Timer.periodic(const Duration(seconds: 20), (_) => charger());
  }

  @override
  void dispose() {
    _sondage?.cancel();
    super.dispose();
  }

  Future<void> charger() async {
    final nombreAvant = state.nombreNonLues;
    state = state.copyWith(chargement: true, erreur: null);
    try {
      final response = await _dio.get('/notifications/mes');
      final notifications =
          (response.data as List<dynamic>).map((n) => AppNotification.fromJson(n as Map<String, dynamic>)).toList();
      state = state.copyWith(chargement: false, notifications: notifications);
      final nombreApres = notifications.where((n) => !n.lue).length;
      if (nombreApres > nombreAvant) {
        SystemSound.play(SystemSoundType.alert);
        HapticFeedback.mediumImpact();
      }
    } on DioException catch (e) {
      state = state.copyWith(chargement: false, erreur: _messageErreur(e));
    }
  }

  Future<void> marquerLue(String id) async {
    final avant = state.notifications;
    state = state.copyWith(
      notifications: [
        for (final n in avant)
          if (n.id == id)
            AppNotification(id: n.id, titre: n.titre, corps: n.corps, type: n.type, referenceId: n.referenceId, lue: true, dateCreation: n.dateCreation)
          else
            n,
      ],
    );
    try {
      await _dio.patch('/notifications/mes/$id/lue');
    } on DioException {
      state = state.copyWith(notifications: avant);
    }
  }

  String _messageErreur(DioException e) {
    if (e.response?.statusCode == 503) return 'Service de notifications momentanément indisponible.';
    return 'Erreur de connexion. Vérifiez votre réseau.';
  }
}

final notificationProvider = StateNotifierProvider<NotificationNotifier, NotificationState>((ref) {
  return NotificationNotifier(ref.watch(dioProvider));
});
