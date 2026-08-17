import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

// Passe par la gateway (port 8088), pas directement service-ida (8081) —
// c'est la gateway qui routera vers le bon microservice à mesure que
// l'app consomme d'autres services (CAP, MKT, etc.)
const String baseUrl = String.fromEnvironment(
  'API_BASE',
  defaultValue: 'http://localhost:8088/api',
);

const String keyAccessToken = 'access_token';
const String keyRefreshToken = 'refresh_token';

// S15 — service-geo (Moteur), axes réels. Appel direct : aucune route
// gateway pour le rôle Chargeur (cf. commit 9c52f02), et l'endpoint
// GET /api/geo/axes?tenantId=... n'exige pas d'authentification.
const String _apiBaseGeo = String.fromEnvironment(
  'API_BASE_GEO',
  defaultValue: 'http://localhost:8084',
);

final geoDioProvider = Provider<Dio>((ref) {
  return Dio(BaseOptions(baseUrl: _apiBaseGeo, connectTimeout: const Duration(seconds: 10)));
});

final dioProvider = Provider<Dio>((ref) {
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
});

Future<bool> _refreshToken(FlutterSecureStorage storage) async {
  try {
    final refreshToken = await storage.read(key: keyRefreshToken);
    if (refreshToken == null) return false;
    final dioRefresh = Dio(BaseOptions(baseUrl: baseUrl));
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
