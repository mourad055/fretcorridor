import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:fretcorridor_chauffeur_transporteur/main.dart';

void main() {
  testWidgets('affiche l\'écran de connexion quand aucune session n\'existe', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: FretCorridorChauffeurApp()));
    await tester.pumpAndSettle();

    expect(find.text('Se connecter'), findsOneWidget);
    expect(find.text('TÉLÉPHONE'), findsOneWidget);
    expect(find.text('CODE'), findsOneWidget);
  });
}
