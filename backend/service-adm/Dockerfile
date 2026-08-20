# Build multi-étapes : chaque service se construit indépendamment (module
# Maven autonome, cf. Plan d'Exécution §2.2) — jamais de dépendance sur les
# autres modules backend/ au moment du build de l'image.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
