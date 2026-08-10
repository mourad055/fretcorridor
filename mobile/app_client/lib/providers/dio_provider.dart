import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

// Il n'existe pas (encore) de gateway unifiée pour l'app Client — celle de
// backend/gateway ne sert que les rôles Web (BUREAU/TRANSPORTEUR/ADMIN) et
// n'a aucune route pour le chargeur. Chaque écran appelle donc directement
// le microservice qui porte réellement son contrat (mêmes DTO, vérifiés) :
// service-ida (auth, KYC), service-mkt (catalogue, demandes, propositions),
// service-not (notifications), service-exe (chronologie), service-flt
// (dernière position). Un --dart-define par service permet de les faire
// pointer ailleurs qu'en local (cf. scripts/run_dev.sh).
const String _apiBaseIda = String.fromEnvironment(
  'API_BASE_IDA',
  defaultValue: 'http://localhost:8081/api',
);
const String _apiBaseMkt = String.fromEnvironment(
  'API_BASE_MKT',
  defaultValue: 'http://localhost:8089/api',
);
const String _apiBaseNot = String.fromEnvironment(
  'API_BASE_NOT',
  defaultValue: 'http://localhost:8094/api',
);
const String _apiBaseExe = String.fromEnvironment(
  'API_BASE_EXE',
  defaultValue: 'http://localhost:8093/api',
);
const String _apiBaseFlt = String.fromEnvironment(
  'API_BASE_FLT',
  defaultValue: 'http://localhost:8092/api',
);

const String keyAccessToken = 'access_token';
const String keyRefreshToken = 'refresh_token';

Dio _creerClient(String baseUrl) {
  const storage = FlutterSecureStorage();
  final dio = Dio(BaseOptions(
    baseUrl: baseUrl,
    connectTimeout: const Duration(seconds: 10),
  ));

  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      final token = await storage.read(key: keyAccessToken);
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      return handler.next(options);
    },
    onError: (error, handler) async {
      if (error.response?.statusCode == 401) {
        final refreshed = await _refreshToken(storage);
        if (refreshed) {
          final token = await storage.read(key: keyAccessToken);
          error.requestOptions.headers['Authorization'] = 'Bearer $token';
          final response = await dio.fetch(error.requestOptions);
          return handler.resolve(response);
        }
      }
      return handler.next(error);
    },
  ));

  return dio;
}

// Le rafraîchissement de token passe toujours par service-ida (seul
// émetteur de tokens), jamais par le service qui a renvoyé le 401.
Future<bool> _refreshToken(FlutterSecureStorage storage) async {
  try {
    final refreshToken = await storage.read(key: keyRefreshToken);
    if (refreshToken == null) return false;
    final dioRefresh = Dio(BaseOptions(baseUrl: _apiBaseIda));
    final response = await dioRefresh.post('/auth/refresh', data: {
      'refreshToken': refreshToken,
    });
    await storage.write(key: keyAccessToken, value: response.data['accessToken']);
    await storage.write(key: keyRefreshToken, value: response.data['refreshToken']);
    return true;
  } catch (e) {
    await storage.delete(key: keyAccessToken);
    await storage.delete(key: keyRefreshToken);
    return false;
  }
}

/// service-ida (8081) — auth, KYC.
final idaDioProvider = Provider<Dio>((ref) => _creerClient(_apiBaseIda));

/// service-mkt (8089 hôte / 8082 conteneur) — catalogue, demandes, propositions.
final mktDioProvider = Provider<Dio>((ref) => _creerClient(_apiBaseMkt));

/// service-not (8094) — notifications.
final notDioProvider = Provider<Dio>((ref) => _creerClient(_apiBaseNot));

/// service-exe (8093) — chronologie de mission.
final exeDioProvider = Provider<Dio>((ref) => _creerClient(_apiBaseExe));

/// service-flt (8092 hôte / 8083 conteneur) — dernière position connue.
final fltDioProvider = Provider<Dio>((ref) => _creerClient(_apiBaseFlt));
