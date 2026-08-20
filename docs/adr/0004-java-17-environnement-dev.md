# ADR 0004 — Java 17 comme cible de compilation (au lieu de 21) sur cet environnement de développement

**Statut** : Accepté

## Contexte

Le Plan d'Exécution v4.0 (§9.1) prévoit Java 21. Sur la machine de développement utilisée pour amorcer `backend/gateway`, seul `openjdk-21-jdk-headless` (JRE, sans `javac`) est installé ; le compilateur disponible est celui d'`openjdk-17-jdk`. Compiler avec `--release 21` échoue donc localement (`error: release version 21 not supported`), alors que `mvn -version` rapporte pourtant un JRE 21 actif.

## Décision

`backend/gateway/pom.xml` cible Java 17 (`<java.version>17</java.version>`), compatible avec le socle minimal de Spring Boot 3.3.x.

## Conséquences

- Aucun impact sur le code applicatif (pas de fonctionnalité Java 21 spécifique utilisée).
- Avant la mise en production ou l'intégration CI/CD réelle, installer `openjdk-21-jdk` (JDK complet, pas seulement le JRE) sur les postes/agents concernés et relever la cible à 21 pour respecter le Plan d'Exécution.
- Chaque nouveau module Maven créé sous `backend/` doit vérifier `javac -version` avant de fixer sa cible, plutôt que de supposer 21 disponible.
