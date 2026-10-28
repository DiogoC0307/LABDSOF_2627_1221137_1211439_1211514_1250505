# Build context is this folder:  docker build -t ghcr.io/<user>/escala-web:latest apps/web
# nginx config is injected by a Kubernetes ConfigMap, so the image is environment-agnostic.
FROM node:24 AS build
WORKDIR /src
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /src/dist/web/browser /usr/share/nginx/html
