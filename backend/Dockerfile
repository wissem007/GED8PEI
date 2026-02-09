# =============================================================================
# Dockerfile Backend GED-PEI
# =============================================================================
# Image multi-stage pour une image finale légère

# -----------------------------------------------------------------------------
# Stage 1: Builder
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copier les fichiers de configuration Maven
COPY pom.xml .
COPY .mvn .mvn 2>/dev/null || true

# Télécharger les dépendances (layer cache)
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Construire l'application
RUN mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# Stage 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

# Métadonnées
LABEL maintainer="GED-PEI Team" \
      version="1.0.0" \
      description="GED-PEI Backend - Spring Boot API"

# Variables d'environnement
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SPRING_PROFILES_ACTIVE=prod \
    TZ=Europe/Paris

# Installer les dépendances système
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

# Créer un utilisateur non-root
RUN addgroup -S gedpei && adduser -S gedpei -G gedpei

# Répertoire de travail
WORKDIR /app

# Copier l'application depuis le builder
COPY --from=builder /build/target/*.jar app.jar

# Créer les répertoires nécessaires
RUN mkdir -p /app/uploads /app/exports /app/logs \
    && chown -R gedpei:gedpei /app

# Utiliser l'utilisateur non-root
USER gedpei

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Exposer le port
EXPOSE 8080

# Point d'entrée
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
