import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

// Passe par la gateway, port 8082 (table de ports figée, cf. docs internes).
// Chemins versionnés par rôle (/api/v1/...) — ne jamais supposer l'ancien
// format /api/{ressource}, cf. AuthController/SecurityConfig côté gateway.
const String baseUrl = String.fromEnvironment(
  'API_BASE',
  defaultValue: 'http://localhost:8082/api/v1',
);

const String keyAccessToken = 'access_token';
const String keyRole = 'role';
const String keyTenantId = 'tenant_id';
const String keyTelephone = 'telephone';

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
  ));

  return dio;
});
