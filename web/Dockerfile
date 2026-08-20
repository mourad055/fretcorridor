# Build multi-étapes, même principe que les Dockerfile backend (cf.
# backend/*/Dockerfile) : build isolé, image finale minimale sans
# toolchain Node/npm.
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# "ng build" (defaultConfiguration production, cf angular.json) produit
# dist/web/browser/ et n'applique PAS le fileReplacement vers
# environment.development.ts : apiBaseUrl reste "/api/v1" (relatif) —
# c'est nginx.conf ci-dessous qui route ce chemin vers le gateway.
FROM nginx:1.27-alpine
COPY --from=build /app/dist/web/browser /usr/share/nginx/html
# .template (pas conf.d/ directement) : l'image officielle nginx lance
# automatiquement envsubst dessus au démarrage (docker-entrypoint.d),
# ce qui résout ${GATEWAY_HOST}/${GATEWAY_PORT} — sans ça nginx essaierait
# de résoudre le nom d'hôte littéral "${GATEWAY_HOST}" et échouerait.
COPY nginx.conf /etc/nginx/templates/default.conf.template
ENV GATEWAY_HOST=gateway
ENV GATEWAY_PORT=8082
EXPOSE 80
