#!/bin/sh

WITHOUT_SCHEMA=${DATABASE_URL#*://}
WITHOUT_USERNAME=${WITHOUT_SCHEMA#*:}

export JDBC_DATABASE_URL=jdbc:postgresql://${DATABASE_URL#*@}
export JDBC_DATABASE_USERNAME=${WITHOUT_SCHEMA%%:*}
export JDBC_DATABASE_PASSWORD=${WITHOUT_USERNAME%%@*}

java -Xmx330m -Xss512k -Dspring.profiles.active=production -jar FootballBot-1.0-SNAPSHOT.jar