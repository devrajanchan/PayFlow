FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY . .
RUN chmod +x mvnw && ./mvnw -q -pl services/payment-intake -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/services/payment-intake/target/payment-intake-*.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
