FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY . .
RUN chmod +x mvnw && ./mvnw -q -pl services/payment-notification -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/services/payment-notification/target/payment-notification-*.jar /app/app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
