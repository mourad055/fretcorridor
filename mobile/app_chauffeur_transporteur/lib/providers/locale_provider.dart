import 'dart:ui';
import 'package:flutter_riverpod/legacy.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

const String keyLangue = 'langue';
const List<String> languesDisponibles = ['fr', 'en'];

/// EF-NOT-05 (CDC, "bilingue FR/EN") : préférence de langue persistée
/// (indépendante de la session — survit à une déconnexion, contrairement au
/// token). Français par défaut si rien n'est encore choisi.
class LocaleNotifier extends StateNotifier<Locale> {
  static const _storage = FlutterSecureStorage();

  LocaleNotifier() : super(const Locale('fr')) {
    _charger();
  }

  Future<void> _charger() async {
    final code = await _storage.read(key: keyLangue);
    if (code != null && languesDisponibles.contains(code)) {
      state = Locale(code);
    }
  }

  Future<void> choisir(String code) async {
    if (!languesDisponibles.contains(code)) return;
    await _storage.write(key: keyLangue, value: code);
    state = Locale(code);
  }
}

final localeProvider = StateNotifierProvider<LocaleNotifier, Locale>((ref) {
  return LocaleNotifier();
});
