# syntax=docker/dockerfile:1.7

# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies first (layer reuse if pom.xml unchanged)
COPY pom.xml ./
RUN mvn -B -e -ntp dependency:go-offline

# Copy sources and build production JAR (frontend assets bundled)
COPY src ./src
COPY frontend ./frontend
RUN mvn -B -e -ntp clean package -Pproduction -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Non-root user for safety
RUN groupadd --system --gid 1001 solar \
 && useradd  --system --uid 1001 --gid solar --shell /sbin/nologin solar

WORKDIR /app

# Copy fat JAR from build stage
COPY --from=build /build/target/solar-dashboard-*.jar /app/app.jar

# Vaadin / Spring Boot defaults
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom" \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=production \
    TZ=Europe/Zurich

EXPOSE 8080

USER solar

# Healthcheck — Spring Boot Actuator is not configured; fall back to TCP probe via /
HEALTHCHECK --interval=60s --timeout=5s --start-period=90s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/ || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
