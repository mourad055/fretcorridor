import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import '../l10n/app_localizations.dart';
import '../theme/app_theme.dart';
import 'login_screen.dart';
import 'inscription_screen.dart';

// Page d'accueil publique — première chose vue par un visiteur.
//
// Vraie vidéo en boucle en arrière-plan (chargement de colis dans une
// camionnette) — illustration statique (hero_illustration.png) conservée
// comme repli le temps que la vidéo s'initialise, pour ne jamais laisser
// un flash noir/vide à l'ouverture (même principe que l'app
// Chauffeur/Transporteur, cf. son welcome_screen.dart).
class WelcomeScreen extends StatefulWidget {
  const WelcomeScreen({super.key});

  @override
  State<WelcomeScreen> createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<WelcomeScreen> {
  late final VideoPlayerController _videoController;

  @override
  void initState() {
    super.initState();
    _videoController = VideoPlayerController.asset('assets/videos/chargement_colis_hero.mp4')
      ..setLooping(true)
      ..setVolume(0)
      ..initialize().then((_) {
        if (!mounted) return;
        setState(() {});
        _videoController.play();
      });
  }

  @override
  void dispose() {
    _videoController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Scaffold(
      backgroundColor: AppColors.fond,
      body: Column(
        children: [
          // ── Zone héro (vidéo/illustration + dégradé de lisibilité) ──
          Expanded(
            flex: 11,
            child: Stack(
              fit: StackFit.expand,
              children: [
                if (_videoController.value.isInitialized)
                  SizedBox.expand(
                    child: FittedBox(
                      fit: BoxFit.cover,
                      child: SizedBox(
                        width: _videoController.value.size.width,
                        height: _videoController.value.size.height,
                        child: VideoPlayer(_videoController),
                      ),
                    ),
                  )
                else
                  Image.asset(
                    'assets/images/hero_illustration.png',
                    fit: BoxFit.cover,
                  ),
                // Dégradé pour que le texte reste lisible sur l'illustration
                Container(
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [Colors.transparent, Color(0xCC1F2937)],
                      stops: [0.35, 1.0],
                    ),
                  ),
                ),
                SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 28),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          margin: const EdgeInsets.only(top: 12),
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(18),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withValues(alpha: 0.15),
                                blurRadius: 14, offset: const Offset(0, 5),
                              ),
                            ],
                          ),
                          child: Image.asset(
                            'assets/images/logo_fretcorridor.jpeg',
                            height: 52,
                          ),
                        ),
                        Padding(
                          padding: const EdgeInsets.only(bottom: 24),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                t.welcomeTitre,
                                style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                                      color: Colors.white, fontSize: 28, height: 1.15,
                                    ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                t.welcomeSousTitre,
                                style: const TextStyle(color: Colors.white70, fontSize: 13, height: 1.4),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),

          // ── Pagination décorative ─────────────────────────
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _Point(actif: true),
                const SizedBox(width: 6),
                _Point(actif: false),
                const SizedBox(width: 6),
                _Point(actif: false),
              ],
            ),
          ),

          // ── Actions ────────────────────────────────────────
          Expanded(
            flex: 6,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  SizedBox(
                    width: double.infinity,
                    height: 54,
                    child: ElevatedButton(
                      onPressed: () => Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const InscriptionScreen()),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.accent,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(t.commencer,
                              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold,
                                  color: AppColors.texteBouton)),
                          const SizedBox(width: 8),
                          const Icon(Icons.arrow_forward, color: Colors.white, size: 18),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(t.dejaUnCompte,
                          style: const TextStyle(color: AppColors.texteMuet, fontSize: 13)),
                      GestureDetector(
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(builder: (_) => const LoginScreen()),
                        ),
                        child: Text(t.connexion,
                            style: const TextStyle(color: AppColors.accent, fontSize: 13,
                                fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Point extends StatelessWidget {
  final bool actif;
  const _Point({required this.actif});

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      width: actif ? 20 : 6,
      height: 6,
      decoration: BoxDecoration(
        color: actif ? AppColors.accent : AppColors.bordure,
        borderRadius: BorderRadius.circular(3),
      ),
    );
  }
}
