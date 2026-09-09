FROM docker.io/library/eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -B -ntp package -DskipTests

FROM docker.io/library/eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/payflow-backend-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
