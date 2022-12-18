FROM openjdk:8-jdk-alpine

ENV MUSL_LOCALE_DEPS cmake make musl-dev gcc gettext-dev libintl
ENV MUSL_LOCPATH /usr/share/i18n/locales/musl

RUN apk add tzdata
RUN apk add --no-cache \
    $MUSL_LOCALE_DEPS \
    && wget https://gitlab.com/rilian-la-te/musl-locales/-/archive/master/musl-locales-master.zip \
    && unzip musl-locales-master.zip \
      && cd musl-locales-master \
      && cmake -DLOCALE_PROFILE=OFF -D CMAKE_INSTALL_PREFIX:PATH=/usr . && make && make install \
      && cd .. && rm -r musl-locales-master

ENV LANG ru_RU.UTF-8
ENV LANGUAGE ru_RU.UTF-8
ENV LC_ALL ru_RU.UTF-8
ENV TZ Europe/Moscow

ADD target/FootballBot-1.0-SNAPSHOT.jar application.jar
ENTRYPOINT ["java","-Xmx330m", "-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8" ,"-Xss512k", "-Xdebug", "-Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n", "-jar", "application.jar"]
