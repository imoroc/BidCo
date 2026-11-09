# Etapa 1: construcción
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Por defecto se construye contra la API local (environment.development.ts).
# Para producción: --build-arg CONFIGURATION=production
ARG CONFIGURATION=development
RUN npx ng build --configuration ${CONFIGURATION}

# Etapa 2: servidor estático
FROM nginxinc/nginx-unprivileged:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 8080
