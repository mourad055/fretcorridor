import 'dart:async';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class _Bandeau {
  final IconData icone;
  final String titre;
  final String description;
  final List<Color> couleurs;

  const _Bandeau({required this.icone, required this.titre, required this.description, required this.couleurs});
}

const _bandeaux = [
  _Bandeau(
    icone: Icons.local_shipping_outlined,
    titre: 'Trouvez des missions rapidement',
    description: 'Déclarez votre capacité, recevez des propositions sur vos axes',
    couleurs: [Color(0xFFDC2626), Color(0xFFB91C1C)],
  ),
  _Bandeau(
    icone: Icons.account_balance_wallet_outlined,
    titre: 'Paiement sécurisé',
    description: 'Suivez vos gains et vos paiements directement dans l\'app',
    couleurs: [Color(0xFF1F2937), Color(0xFF111827)],
  ),
  _Bandeau(
    icone: Icons.gps_fixed,
    titre: 'Suivi GPS en temps réel',
    description: 'Partagez votre position pendant vos missions',
    couleurs: [Color(0xFFB45309), Color(0xFF92400E)],
  ),
];

// Même carrousel promotionnel que l'app Client (image défilante avec
// indicateurs), contenu adapté au rôle Chauffeur/Transporteur.
class PromoCarousel extends StatefulWidget {
  const PromoCarousel({super.key});

  @override
  State<PromoCarousel> createState() => _PromoCarouselState();
}

class _PromoCarouselState extends State<PromoCarousel> {
  final _controller = PageController();
  Timer? _timer;
  int _page = 0;

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 4), (_) {
      if (!mounted) return;
      _page = (_page + 1) % _bandeaux.length;
      _controller.animateToPage(_page, duration: const Duration(milliseconds: 500), curve: Curves.easeInOut);
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        SizedBox(
          height: 130,
          child: PageView.builder(
            controller: _controller,
            onPageChanged: (i) => setState(() => _page = i),
            itemCount: _bandeaux.length,
            itemBuilder: (context, i) {
              final b = _bandeaux[i];
              return Container(
                margin: const EdgeInsets.symmetric(horizontal: 4),
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  gradient: LinearGradient(colors: b.couleurs, begin: Alignment.topLeft, end: Alignment.bottomRight),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 48, height: 48,
                      decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.15), shape: BoxShape.circle),
                      child: Icon(b.icone, color: Colors.white, size: 24),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(b.titre, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
                          const SizedBox(height: 4),
                          Text(b.description, style: const TextStyle(color: Colors.white70, fontSize: 11), maxLines: 2),
                        ],
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        ),
        const SizedBox(height: 10),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(_bandeaux.length, (i) {
            final actif = i == _page;
            return AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              margin: const EdgeInsets.symmetric(horizontal: 3),
              width: actif ? 18 : 6,
              height: 6,
              decoration: BoxDecoration(
                color: actif ? AppColors.accent : AppColors.bordure,
                borderRadius: BorderRadius.circular(3),
              ),
            );
          }),
        ),
      ],
    );
  }
}
