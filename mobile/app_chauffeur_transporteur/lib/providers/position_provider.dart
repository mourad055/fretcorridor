import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/legacy.dart';
import 'package:geolocator/geolocator.dart';
import 'dio_provider.dart';

const _intervalleEnvoi = Duration(seconds: 30);

class PositionState {
  final bool suiviActif;
  final String? missionId;
  final DateTime? dernierEnvoi;
  final String? erreur;

  const PositionState({this.suiviActif = false, this.missionId, this.dernierEnvoi, this.erreur});

  PositionState copyWith({bool? suiviActif, String? missionId, DateTime? dernierEnvoi, String? erreur}) {
    return PositionState(
      suiviActif: suiviActif ?? this.suiviActif,
      missionId: missionId ?? this.missionId,
      dernierEnvoi: dernierEnvoi ?? this.dernierEnvoi,
      erreur: erreur,
    );
  }
}

// S6 (EF-TRK-01) : capture + envoi périodique en arrière-plan pendant qu'une
// mission est active — voir EnvoiPositionController (gateway) et
// PositionController (service-flt). démarrerSuivi(missionId) est conçu pour
// être appelé automatiquement par l'écran de mission en cours (S7) une fois
// disponible ; en attendant, l'écran expose une saisie manuelle du missionId.
class PositionNotifier extends StateNotifier<PositionState> {
  final Dio _dio;
  Timer? _timer;

  PositionNotifier(this._dio) : super(const PositionState());

  void demarrerSuivi(String missionId) {
    _timer?.cancel();
    state = state.copyWith(suiviActif: true, missionId: missionId, erreur: null);
    _envoyerUnePosition();
    _timer = Timer.periodic(_intervalleEnvoi, (_) => _envoyerUnePosition());
  }

  void arreterSuivi() {
    _timer?.cancel();
    _timer = null;
    state = state.copyWith(suiviActif: false);
  }

  Future<void> _envoyerUnePosition() async {
    final missionId = state.missionId;
    if (missionId == null) return;

    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied || permission == LocationPermission.deniedForever) {
      state = state.copyWith(erreur: 'Position GPS indisponible — activez la localisation.');
      return;
    }

    try {
      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(accuracy: LocationAccuracy.high),
      );
      await _dio.post('/positions', data: {
        'missionId': missionId,
        'latitude': position.latitude,
        'longitude': position.longitude,
        'horodatage': DateTime.now().toIso8601String(),
      });
      state = state.copyWith(dernierEnvoi: DateTime.now(), erreur: null);
    } on DioException catch (e) {
      state = state.copyWith(erreur: _messageErreur(e));
    } catch (_) {
      state = state.copyWith(erreur: 'Position GPS indisponible.');
    }
  }

  String _messageErreur(DioException e) {
    final status = e.response?.statusCode;
    if (status == 400) {
      return (e.response?.data is Map ? e.response?.data['detail'] as String? : null) ?? 'Position refusée.';
    }
    return 'Erreur réseau — nouvelle tentative au prochain envoi.';
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}

final positionProvider = StateNotifierProvider<PositionNotifier, PositionState>((ref) {
  return PositionNotifier(ref.watch(dioProvider));
});
