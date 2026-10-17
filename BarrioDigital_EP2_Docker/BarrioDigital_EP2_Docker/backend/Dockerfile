# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S -G app app && mkdir -p /app/data && chown -R app:app /app
WORKDIR /app
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -Duser.timezone=America/Santiago"
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=8 CMD wget -q -O /dev/null http://127.0.0.1:${SERVER_PORT}/actuator/health/liveness || exit 1
ENTRYPOINT ["java","-jar","/app/app.jar"]

FROM runtime AS bff
ENV SERVER_PORT=8081
COPY --from=build --chown=app:app /build/bff/target/bff-1.0.0.jar /app/app.jar
EXPOSE 8081

FROM runtime AS catalog
ENV SERVER_PORT=8082
COPY --from=build --chown=app:app /build/catalog/target/catalog-1.0.0.jar /app/app.jar
EXPOSE 8082

FROM runtime AS requests
ENV SERVER_PORT=8083
COPY --from=build --chown=app:app /build/requests/target/requests-1.0.0.jar /app/app.jar
EXPOSE 8083

FROM runtime AS notify
ENV SERVER_PORT=8084
COPY --from=build --chown=app:app /build/notify/target/notify-1.0.0.jar /app/app.jar
EXPOSE 8084

FROM runtime AS rabbitadmin
ENV SERVER_PORT=8085
COPY --from=build --chown=app:app /build/rabbitadmin/target/rabbitadmin-1.0.0.jar /app/app.jar
EXPOSE 8085
