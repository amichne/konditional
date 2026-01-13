FROM node:18 AS ui-build
WORKDIR /app/konditional-generated-ui
COPY konditional-generated-ui/ konditional-generated-ui/package-lock.json ./
RUN npm install && npm run build

#FROM gradle:7.6.0-jdk17 AS backend-build

FROM zulu-openjdk:21-latest as backend-build
WORKDIR /app
COPY ./demo/demo-0.1.0/ ./demo/
COPY --from=ui-build /app/konditional-generated-ui/dist ./konditional-generated-ui/dist

EXPOSE 8080

ENTRYPOINT ["shell", "-c", "echo Starting Konditional Demo... && ./demo/demo-0.1.0/bin/demo"]
