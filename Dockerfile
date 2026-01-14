FROM node:18 AS ui-build
WORKDIR /app/konditional-generated-ui
COPY konditional-generated-ui/ konditional-generated-ui/package-lock.json ./
RUN npm install && npm run build

#FROM gradle:7.6.0-jdk17 AS backend-build

FROM eclipse-temurin:21-jre-alpine-3.23 as backend-build
WORKDIR /app
COPY ./demo/build/distributions/demo-0.1.0.zip ./demo/demo-0.1.0.zip
COPY --from=ui-build /app/konditional-generated-ui/dist ./konditional-generated-ui/dist

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "echo Starting Konditional Demo... && unzip -oq demo/demo-0.1.0.zip -d demo_temp && cd demo_temp/demo-0.1.0 && ./bin/demo"]
