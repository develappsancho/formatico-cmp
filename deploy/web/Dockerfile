# syntax=docker/dockerfile:1
# Formatico's web image: the web app's files behind nginx. The distribution, its Wasm build and the JavaScript one its
# loader falls back to, is given to the build as the context `app` (compose.local.yaml): Gradle has made it on this
# machine, and nothing here compiles.

# Every file worth it is compressed once, here, so that nginx sends the .gz beside a file instead of compressing it for
# each request. The source maps stay out.
FROM --platform=$BUILDPLATFORM alpine:3.24 AS files
COPY --from=app / /app/
RUN rm -f /app/*.map \
    && find /app -type f -size +1k \( -name '*.html' -o -name '*.css' -o -name '*.js' -o -name '*.wasm' \
        -o -name '*.json' -o -name '*.webmanifest' -o -name '*.svg' -o -name '*.ttf' -o -name '*.txt' \) \
        -exec gzip -9 -k -n {} +

FROM nginx:1.30-alpine
ARG REVISION=unknown
LABEL org.opencontainers.image.title="Formatico web" \
      org.opencontainers.image.description="Formatico's web app, served by nginx, with its server at /api" \
      org.opencontainers.image.revision="$REVISION"
COPY default.conf /etc/nginx/conf.d/default.conf
COPY --from=files /app/ /usr/share/nginx/html/
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
