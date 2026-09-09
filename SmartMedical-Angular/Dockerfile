# ============================================================
# Angular Frontend Dockerfile - Multi-stage build
# ============================================================
FROM node:20-alpine AS builder

WORKDIR /app

# Copier package.json et installer les dépendances
COPY package*.json ./
RUN npm ci --legacy-peer-deps

# Copier le code source et builder en production
COPY . .
RUN npm run build -- --configuration production

# ============================================================
# Serveur Nginx léger pour servir l'application Angular
# ============================================================
FROM nginx:alpine

# Supprimer la config nginx par défaut
RUN rm -rf /usr/share/nginx/html/*

# Copier les fichiers buildés (Angular 19 met les fichiers dans dist/smartmedical-angular/browser)
COPY --from=builder /app/dist/smartmedical-angular/browser /usr/share/nginx/html

# Configuration Nginx pour Angular (SPA routing)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
