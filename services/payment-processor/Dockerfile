FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY . .
RUN chmod +x mvnw && ./mvnw -q -pl services/payment-processor -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/services/payment-processor/target/payment-processor-*.jar /app/app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
