import 'package:dio/dio.dart';
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

class NotificationNotifier extends StateNotifier<NotificationState> {
  final Dio _dio;
  NotificationNotifier(this._dio) : super(const NotificationState()) {
    charger();
  }

  Future<void> charger() async {
    state = state.copyWith(chargement: true);
    try {
      final responseListe = await _dio.get('/notifications');
      final responseNombre = await _dio.get('/notifications/non-lues/nombre');
      state = state.copyWith(
        chargement: false,
        notifications: (responseListe.data as List).map((e) => NotificationModel.fromJson(e)).toList(),
        nombreNonLues: responseNombre.data['nombre'] ?? 0,
      );
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
