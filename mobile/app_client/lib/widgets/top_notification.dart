import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

// Notification transitoire affichée en haut de l'écran (sous la barre de
// statut) plutôt qu'un SnackBar Material classique, ancré en bas — remplace
// tous les showSnackBar de l'app pour un comportement homogène et conforme à
// la demande explicite : "les erreurs et notifications doivent venir par
// le haut".
void afficherNotification(
  BuildContext context, {
  required String message,
  Color couleur = AppColors.accent,
  IconData icone = Icons.info_outline,
  Duration duree = const Duration(seconds: 4),
}) {
  final overlay = Overlay.of(context, rootOverlay: true);
  late OverlayEntry entry;
  entry = OverlayEntry(
    builder: (context) => _TopNotificationBanner(
      message: message,
      couleur: couleur,
      icone: icone,
      duree: duree,
      onFin: () => entry.remove(),
    ),
  );
  overlay.insert(entry);
}

class _TopNotificationBanner extends StatefulWidget {
  final String message;
  final Color couleur;
  final IconData icone;
  final Duration duree;
  final VoidCallback onFin;

  const _TopNotificationBanner({
    required this.message,
    required this.couleur,
    required this.icone,
    required this.duree,
    required this.onFin,
  });

  @override
  State<_TopNotificationBanner> createState() => _TopNotificationBannerState();
}

class _TopNotificationBannerState extends State<_TopNotificationBanner> with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<Offset> _offset;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(vsync: this, duration: const Duration(milliseconds: 250));
    _offset = Tween<Offset>(begin: const Offset(0, -1), end: Offset.zero)
        .animate(CurvedAnimation(parent: _controller, curve: Curves.easeOut));
    _controller.forward();
    Future.delayed(widget.duree, () async {
      if (!mounted) return;
      await _controller.reverse();
      widget.onFin();
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Positioned(
      top: 0, left: 0, right: 0,
      child: SafeArea(
        child: SlideTransition(
          position: _offset,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 10, 12, 0),
            child: Material(
              color: Colors.transparent,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                decoration: BoxDecoration(
                  color: widget.couleur,
                  borderRadius: BorderRadius.circular(12),
                  boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.2), blurRadius: 10, offset: const Offset(0, 4))],
                ),
                child: Row(children: [
                  Icon(widget.icone, color: Colors.white, size: 20),
                  const SizedBox(width: 10),
                  Expanded(child: Text(widget.message, style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600))),
                ]),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
