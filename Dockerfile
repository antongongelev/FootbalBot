FROM maven:3.8.4-openjdk-8-slim AS build-env
COPY . /football_bot
WORKDIR /football_bot
SHELL ["/bin/bash", "-c"]
RUN source ./scripts/define-heroku-variables.sh

RUN printenv

RUN mvn clean package spring-boot:repackage
RUN mvn liquibase:update -Dliquibase.propertyFile=application.production.yml -Dliquibase.propertyFileWillOverride=true

FROM bellsoft/liberica-openjdk-alpine:8u322 as final
RUN adduser -S user
WORKDIR /bot
COPY --from=build-env /football_bot/target/FootballBot-1.0-SNAPSHOT.jar .
# Run under non-privileged user with minimal write permissions
USER user
ENTRYPOINT ["java", "-Xmx330m", "-Xss512k", "-Dspring.profiles.active=production", "-jar", "FootballBot-1.0-SNAPSHOT.jar"]
# Expose dummy port to avoid Heroku errors
ENV PORT=8080
EXPOSE $PORT
