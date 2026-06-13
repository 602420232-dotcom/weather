FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY backend-spring/pom.xml backend-spring/
COPY backend-spring/src backend-spring/src
RUN mvn clean package -DskipTests -B -pl backend-spring -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.connectionTimeout=60000 -Dmaven.wagon.http.socketTimeout=60000

FROM eclipse-temurin:17-jre
RUN groupadd -r appuser && useradd -r -g appuser -m -s /sbin/nologin appuser
WORKDIR /app
COPY --from=builder /build/backend-spring/target/*.jar app.jar
RUN chown appuser:appuser app.jar
EXPOSE 8080
USER appuser
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health || wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
