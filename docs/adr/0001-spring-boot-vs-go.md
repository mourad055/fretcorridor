# ADR 0001 — Spring Boot en microservices plutôt que Go

**Statut** : Accepté (décision d'équipe, héritée du Plan d'Exécution v4.0 §1)

## Contexte

Le CDC v4.0 (§12.3) recommande des services Go pour la performance et la concurrence native sur l'ingestion de positions.

## Décision

L'équipe retient Spring Boot (Java 21) en microservices, cohérent avec les compétences déjà maîtrisées (Spring Boot/JPA) et avec le choix fait dès la v3.0.

## Conséquences

Les deux principes structurants du CDC sont conservés à l'identique :
- Services découplés par le bus d'événements Kafka (jamais de monolithe).
- Moteur d'optimisation (`service-opt`) en service autonome, mis à l'échelle indépendamment, dont l'indisponibilité dégrade sans interrompre (ENF-DIS-04).

Seul le langage change. Voir Plan d'Exécution v4.0 §1 et §4.
