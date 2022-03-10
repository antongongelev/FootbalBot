FROM maven:3.8.4-openjdk-8-slim AS build-env
COPY . /football_bot
WORKDIR /football_bot
RUN mvn clean package spring-boot:repackage

ARG JDBC_DATABASE_URL=?
ENV JDBC_DATABASE_URL=$JDBC_DATABASE_URL

ARG JDBC_DATABASE_USERNAME=?
ENV JDBC_DATABASE_USERNAME=$JDBC_DATABASE_USERNAME

ARG JDBC_DATABASE_PASSWORD=?
ENV JDBC_DATABASE_PASSWORD=$JDBC_DATABASE_PASSWORD

RUN printenv

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
