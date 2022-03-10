#!/bin/sh

x=$tempvar
y=${x#*://}
z=${y#*:}

export JDBC_DATABASE_URL=jdbc:postgresql://${x#*@}
export JDBC_DATABASE_USERNAME=${y%%:*}
export JDBC_DATABASE_PASSWORD=${z%%@*}
