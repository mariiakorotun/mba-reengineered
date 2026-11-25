# Багатоетапна збірка: JDK лише на етапі build, у runtime — JRE і непривілейований користувач.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 mba
WORKDIR /app
COPY --from=build /src/target/mba-bank-reengineered-*.jar app.jar
USER mba
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
