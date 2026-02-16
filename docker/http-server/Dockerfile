FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle :konditional-http-server:installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/konditional-http-server/build/install/konditional-http-server /app
RUN mkdir -p /data
EXPOSE 8080
ENV PORT=8080
ENV STORAGE_PATH=/data/snapshots.json
VOLUME ["/data"]
ENTRYPOINT ["/app/bin/konditional-http-server"]
