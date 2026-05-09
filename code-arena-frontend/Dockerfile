# ============================================================
# CodeArena Frontend — Multi-stage lightweight build
# Final image: ~11MB (nginx:alpine-slim + compiled Angular)
# ============================================================

# --- Stage 1: Build Angular app ---
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files first (cache npm install)
COPY package.json package-lock.json ./
RUN npm ci --prefer-offline

# Copy source and build for production
COPY . .
RUN npx ng build --configuration=production

# --- Stage 2: Minimal NGINX runtime ---
FROM nginx:1.27-alpine-slim
LABEL maintainer="CodeArena Team"

# Remove default nginx config and page
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built Angular files
# Angular 18 with application builder outputs to dist/<project>/browser
COPY --from=build /app/dist/code-arena-frontend/browser /usr/share/nginx/html

# Security: run nginx as non-root
RUN chown -R nginx:nginx /usr/share/nginx/html \
    && chown -R nginx:nginx /var/cache/nginx \
    && chown -R nginx:nginx /var/log/nginx \
    && touch /var/run/nginx.pid \
    && chown nginx:nginx /var/run/nginx.pid

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:80/ || exit 1
