# =============================================================================
# Dockerfile Frontend GED-PEI
# =============================================================================
# Image multi-stage pour une image finale légère

# -----------------------------------------------------------------------------
# Stage 1: Builder
# -----------------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /build

# Copier les fichiers de configuration
COPY package*.json ./

# Installer les dépendances
RUN npm ci

# Copier le code source
COPY . .

# Variables d'environnement pour le build
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL

# Construire l'application
RUN npm run build

# -----------------------------------------------------------------------------
# Stage 2: Runtime avec Nginx
# -----------------------------------------------------------------------------
FROM nginx:alpine AS runtime

# Métadonnées
LABEL maintainer="GED-PEI Team" \
      version="1.0.0" \
      description="GED-PEI Frontend - React Application"

# Supprimer la configuration par défaut
RUN rm -rf /usr/share/nginx/html/*

# Copier la configuration Nginx personnalisée
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copier l'application buildée depuis le builder
COPY --from=builder /build/dist /usr/share/nginx/html

# Créer un script pour injecter les variables d'environnement
RUN echo '#!/bin/sh' > /docker-entrypoint.d/40-env-config.sh && \
    echo 'if [ -n "$VITE_API_URL" ]; then' >> /docker-entrypoint.d/40-env-config.sh && \
    echo '  sed -i "s|__API_URL__|$VITE_API_URL|g" /usr/share/nginx/html/index.html' >> /docker-entrypoint.d/40-env-config.sh && \
    echo 'fi' >> /docker-entrypoint.d/40-env-config.sh && \
    chmod +x /docker-entrypoint.d/40-env-config.sh

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://localhost:80/health || exit 1

# Exposer le port
EXPOSE 80

# Commande par défaut (héritée de l'image nginx)
CMD ["nginx", "-g", "daemon off;"]
