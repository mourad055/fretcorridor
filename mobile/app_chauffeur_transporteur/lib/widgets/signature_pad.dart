import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import '../theme/app_theme.dart';

/// RG-070/EF-EXE-03 : signature tactile du tiers (destinataire/expéditeur)
/// à la prise en charge/livraison. Pas de dépendance externe (`signature`
/// sur pub.dev) : un pad dessiné à la main via CustomPainter suffit pour ce
/// besoin ponctuel, capturé en PNG via RenderRepaintBoundary — évite
/// d'introduire un nouveau package pour un widget aussi simple.
class SignaturePad extends StatefulWidget {
  const SignaturePad({super.key});

  @override
  SignaturePadState createState() => SignaturePadState();
}

class SignaturePadState extends State<SignaturePad> {
  final List<Offset?> _points = [];
  final GlobalKey _repaintKey = GlobalKey();

  bool get estVide => _points.isEmpty;

  void effacer() => setState(() => _points.clear());

  /// Capture le tracé en PNG. Retourne null si rien n'a été dessiné.
  Future<Uint8List?> capturerPng() async {
    if (estVide) return null;
    final boundary = _repaintKey.currentContext?.findRenderObject() as RenderRepaintBoundary?;
    if (boundary == null) return null;
    final image = await boundary.toImage(pixelRatio: 2.0);
    final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
    return byteData?.buffer.asUint8List();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 180,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.bordure),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(10),
        child: RepaintBoundary(
          key: _repaintKey,
          child: GestureDetector(
            onPanUpdate: (details) {
              final box = context.findRenderObject() as RenderBox;
              setState(() => _points.add(box.globalToLocal(details.globalPosition)));
            },
            onPanEnd: (_) => setState(() => _points.add(null)),
            child: CustomPaint(
              painter: _TraitPainter(_points),
              size: Size.infinite,
            ),
          ),
        ),
      ),
    );
  }
}

class _TraitPainter extends CustomPainter {
  final List<Offset?> points;
  _TraitPainter(this.points);

  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawRect(Offset.zero & size, Paint()..color = Colors.white);
    final peinture = Paint()
      ..color = Colors.black87
      ..strokeWidth = 2.5
      ..strokeCap = StrokeCap.round;
    for (int i = 0; i < points.length - 1; i++) {
      final a = points[i];
      final b = points[i + 1];
      if (a != null && b != null) {
        canvas.drawLine(a, b, peinture);
      }
    }
  }

  @override
  bool shouldRepaint(_TraitPainter oldDelegate) => true;
}
