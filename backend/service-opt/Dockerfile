FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copier d'abord le pom.xml parent (common-libs) puis le pom.xml du service
COPY common-libs/pom.xml common-libs/
COPY service-opt/pom.xml service-opt/

# Copier le code source de common-libs puis du service
COPY common-libs/src common-libs/src/
COPY service-opt/src service-opt/src/

# Build common-libs en premier, puis le service
RUN cd common-libs && mvn clean install -DskipTests -B && cd ..
RUN cd service-opt && mvn clean package -DskipTests -B && cd ..

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/service-opt/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
