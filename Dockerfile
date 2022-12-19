FROM openjdk:8-jdk-alpine
RUN apk add tzdata
ENV TZ Europe/Moscow
ADD target/FootballBot-1.0-SNAPSHOT.jar application.jar
ENTRYPOINT ["java","-Xmx330m","-Xss512k", "-Xdebug", "-Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n", "-jar", "application.jar"]
