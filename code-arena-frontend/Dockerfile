# syntax=docker/dockerfile:1.6

# ---------- Stage 1: build ----------
FROM node:20-alpine AS build
WORKDIR /app

# Install deps first to maximize layer cache
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

# Copy the rest of the sources
COPY . .

# Production build (angular.json has fileReplacements: environment.ts -> environment.prod.ts)
RUN npx ng build --configuration=production

# ---------- Stage 2: runtime ----------
FROM nginx:1.27-alpine-slim AS runtime

# Replace default site config with our SPA config
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Angular 18 application builder writes to dist/<project>/browser
COPY --from=build /app/dist/code-arena-frontend/browser /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q --spider http://127.0.0.1/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
