FROM maven:3.8.4-openjdk-8-slim AS build-env
COPY . /football_bot
WORKDIR /football_bot
RUN mvn clean package spring-boot:repackage

FROM bellsoft/liberica-openjdk-alpine:8u322 as final
RUN adduser -S user
WORKDIR /bot
COPY --from=build-env /football_bot/target/FootballBot-1.0-SNAPSHOT.jar .
COPY --from=build-env /football_bot/scripts/define-heroku-variables.sh .
COPY --from=build-env /football_bot/scripts/entrypoint.sh .
# Run under non-privileged user with minimal write permissions
USER user
ENTRYPOINT ["entrypoint.sh"]
# Expose dummy port to avoid Heroku errors
ENV PORT=8080
EXPOSE $PORT
