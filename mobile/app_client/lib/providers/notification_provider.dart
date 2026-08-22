import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/legacy.dart';
import '../models/notification_model.dart';
import 'dio_provider.dart';

class NotificationState {
  final List<NotificationModel> notifications;
  final int nombreNonLues;
  final bool chargement;

  const NotificationState({this.notifications = const [], this.nombreNonLues = 0, this.chargement = false});

  NotificationState copyWith({List<NotificationModel>? notifications, int? nombreNonLues, bool? chargement}) {
    return NotificationState(
      notifications: notifications ?? this.notifications,
      nombreNonLues: nombreNonLues ?? this.nombreNonLues,
      chargement: chargement ?? this.chargement,
    );
  }
}

// Aucun push FCM (pas de projet Firebase disponible, cf. NotificationService
// javadoc côté service-not) — le seul moyen de savoir qu'une notification
// est arrivée pendant que l'app est ouverte est de sonder périodiquement.
// Retour utilisateur direct (22 août) : une notification créée côté serveur
// restait invisible tant que l'écran Notifications n'était pas rouvert
// manuellement — sondage + son/vibration à la première hausse du compteur.
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
    state = state.copyWith(chargement: true);
    try {
      final responseListe = await _dio.get('/notifications');
      final responseNombre = await _dio.get('/notifications/non-lues/nombre');
      final nouveauNombre = (responseNombre.data['nombre'] ?? 0) as int;
      state = state.copyWith(
        chargement: false,
        notifications: (responseListe.data as List).map((e) => NotificationModel.fromJson(e)).toList(),
        nombreNonLues: nouveauNombre,
      );
      if (nouveauNombre > nombreAvant) {
        SystemSound.play(SystemSoundType.alert);
        HapticFeedback.mediumImpact();
      }
    } on DioException {
      state = state.copyWith(chargement: false);
    }
  }

  Future<void> marquerCommeLue(String id) async {
    try {
      await _dio.patch('/notifications/$id/lue');
      await charger();
    } on DioException {
      // Pas grave si ça échoue — l'utilisateur peut réessayer, pas bloquant
    }
  }
}

final notificationProvider = StateNotifierProvider<NotificationNotifier, NotificationState>((ref) {
  return NotificationNotifier(ref.watch(notDioProvider));
});
